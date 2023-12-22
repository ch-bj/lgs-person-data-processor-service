package ch.ejpd.lgs.searchindex.client.service.sync;

import ch.ejpd.lgs.searchindex.client.entity.LandRegister;
import ch.ejpd.lgs.searchindex.client.entity.Setting;
import ch.ejpd.lgs.searchindex.client.repository.LandRegisterRepository;
import ch.ejpd.lgs.searchindex.client.repository.SettingRepository;
import lombok.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FullSyncSettingsStore {
  private final SettingRepository settingRepository;
  private final LandRegisterRepository landRegisterRepository;

  public FullSyncSettingsStore(SettingRepository settingRepository, LandRegisterRepository landRegisterRepository) {
    this.settingRepository = settingRepository;
      this.landRegisterRepository = landRegisterRepository;
  }

  @Transactional
  public String loadPersistedSetting(@NonNull FullSyncSettings key) {
    return settingRepository
        .findByKey(key.toString())
        .map(Setting::getValue)
        .orElse(key.getDefaultValue());
  }

  @Transactional
  public void persistSetting(@NonNull FullSyncSettings key, String value) {
    final Setting setting =
        settingRepository
            .findByKey(key.toString())
            .orElse(Setting.builder().key(key.toString()).build());
    setting.setValue(value);
    settingRepository.save(setting);
  }

  public Map<String, Integer> loadPersistedLandRegisterSetting(String senderId) {
    return landRegisterRepository.getAllBySenderId(senderId).stream()
            .filter(lr -> lr.getMessages() > 0)
            .collect(Collectors.toMap(LandRegister::getKey, LandRegister::getMessages));
  }

  public void persistLandRegisterSetting(Map<String, Integer> settings, String senderId) {
    Set<String> landRegisters = settings.keySet();
    List<LandRegister> landRegisterEntities = landRegisters.stream()
            .map(lr -> LandRegister.builder()
                    .key(lr)
                    .senderId(senderId)
                    .messages(settings.get(lr))
                    .build())
            .toList();
    landRegisterRepository.saveAll(landRegisterEntities);
  }

  public void clearLandRegisterSetting(String senderId) {
    landRegisterRepository.deleteAllBySenderId(senderId);
  }
}
