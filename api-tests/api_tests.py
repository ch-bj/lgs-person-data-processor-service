#!/usr/bin/python
import requests
from os.path import isfile, isdir
import logging
import time
import configparser
import json
import glob

from enum import Enum


class ExpectedResults(Enum):
    VALID = 1
    INVALID = 2


class TransactionStates(Enum):
    NEW = 1
    PROCESSED = 2
    FAILED = 3
    JOB_ASSOCIATED = 4
    JOB_SENT = 5
    JOB_RECEIVED = 6


completion_states = [TransactionStates.JOB_ASSOCIATED,
                     TransactionStates.PROCESSED,
                     TransactionStates.FAILED]

config = configparser.ConfigParser()
config.read('api_tests.properties')

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger()


def expected_states(expected_result: ExpectedResults):
    if expected_result == ExpectedResults.VALID:
        return [TransactionStates.JOB_ASSOCIATED,
                TransactionStates.PROCESSED]
    else:
        return [TransactionStates.FAILED]


def load_json(filename):
    json_file = open(filename)
    return json_file.read()


def load_test_suites():
    assert isdir(config["test-data"]["test-cases.path"])

    if "test-cases.file" in config["test-data"]:
        test_suite = config["test-data"]["test-cases.path"] + "/" + \
                     config["test-data"][
                         "test-cases.file"]
        assert isfile(
            test_suite), "Test suite " + test_suite + " does not exist"
        return [load_json(test_suite)]
    else:
        return map(load_json,
                   glob.glob(
                       config["test-data"]["test-cases.path"] + "/*.json"))


def send_data_and_validate_case(test_case):
    expected_result = ExpectedResults[test_case["expectedResult"]]
    expected_state_list = expected_states(expected_result)

    logger.info("Name: " + test_case["testCaseName"])
    logger.info("Description: " + test_case["testCaseDescription"])
    logger.debug("JsonPayloadData: " + test_case["payload"])
    logger.info("Expected result: " + str(expected_result))
    response = requests.post(
        config["endpoints"]["endpoint.job.partial.url"],
        headers=json.loads(
            config["endpoints"]["endpoint.authentication.headers"]),
        data=test_case["payload"])
    assert response.status_code == 200
    transaction_id = response.json()["transactionId"]
    logger.info("Sent dataset. Got TransactionId: " + transaction_id)
    logger.info(
        "Query transaction log and wait for completion, expecting state "
        + str(expected_state_list) + "...")
    state = wait_for_completion(transaction_id, expected_state_list)
    logger.info("Completed with state: " + str(state))
    assert state in expected_state_list, "Expected states " + str(
        expected_state_list) + "but actual state was " + str(state)

    return transaction_id


def get_transaction(transaction_id):
    response = requests.get(config["endpoints"][
                                "endpoint.job.transaction.url"] + "/"
                            + transaction_id,
                            headers=json.loads(config["endpoints"][
                                                   "endpoint.authentication.headers"]))
    assert response.status_code == 200
    return response.json()


def wait_for_completion(transaction_id, completion_states):
    transaction = get_transaction(transaction_id)
    logger.debug("TransactionData: " + str(transaction))
    processing = True
    trials = 0
    while processing and trials < int(
        config["test-parameters"]["query-transaction.num-of-retries"]):
        trials = trials + 1
        transaction_state = TransactionStates[transaction['state']]
        if transaction_state in completion_states:
            return transaction_state
        else:
            logger.debug("TransactionStatus: " + str(
                TransactionStates[transaction['state']]))
            time.sleep(float(
                config["test-parameters"]["query-transaction.retry-wait-time"]))
            transaction = get_transaction(transaction_id)
    assert False, "Did not get expected completion states " + str(
        completion_states) + " but " + str(transaction_state) + " after " + \
                  config["test-parameters"][
                      "query-transaction.num-of-retries"] + " retries."


def run_test_suite(test_suite):
    logger.info(
        "*****Running test suite: " + test_suite["testSuiteName"] + "******")
    logger.info("Test suite description: " + test_suite["testSuiteDescription"])
    transaction_ids = []
    for test_case in test_suite["testCases"]:
        transaction_id = send_data_and_validate_case(test_case)
        transaction_ids.append(transaction_id)
    return transaction_ids


def test_run_all_test_suites():
    test_suites = load_test_suites()
    for test_suite in test_suites:
        json_data = json.loads(test_suite)
        run_test_suite(json_data)


test_run_all_test_suites()