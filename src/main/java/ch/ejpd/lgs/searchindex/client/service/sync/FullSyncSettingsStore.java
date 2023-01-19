package ch.ejpd.lgs.searchindex.client.service.sync;

import ch.ejpd.lgs.searchindex.client.entity.Setting;
import ch.ejpd.lgs.searchindex.client.repository.SettingRepository;
import lombok.NonNull;
import org.springframework.transaction.annotation.Transactional;

public class FullSyncSettingsStore {
  private final SettingRepository settingRepository;

  public FullSyncSettingsStore(SettingRepository settingRepository) {
    this.settingRepository = settingRepository;
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
}
