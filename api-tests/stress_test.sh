#!/bin/bash

if [ $# -ne 2 ]
then
    echo "stress_test.sh [REPEAT_COUNT] [PARALLEL_WORKERS]"
    exit 1
fi

echo "running $1 repeats of tests with $2 parallel workers"
pytest --count=$1 -n=$2 api_tests.py
