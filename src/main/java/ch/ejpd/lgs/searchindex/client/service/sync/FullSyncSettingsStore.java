package ch.ejpd.lgs.searchindex.client.service.sync;

import ch.ejpd.lgs.searchindex.client.entity.Setting;
import ch.ejpd.lgs.searchindex.client.repository.SettingRepository;
import lombok.NonNull;
import org.springframework.transaction.annotation.Transactional;

/**
 * Utility class for loading and persisting settings related to full synchronization.
 */
public class FullSyncSettingsStore {
  private final SettingRepository settingRepository;

  /**
   * Constructor for FullSyncSettingsStore.
   *
   * @param settingRepository The repository for accessing and modifying settings.
   */
  public FullSyncSettingsStore(SettingRepository settingRepository) {
    this.settingRepository = settingRepository;
  }

  /**
   * Loads a persisted setting from the repository.
   *
   * @param key The key of the setting to be loaded.
   * @return The value of the setting if found, otherwise the default value associated with the key.
   */
  @Transactional
  public String loadPersistedSetting(@NonNull FullSyncSettings key) {
    return settingRepository
        .findByKey(key.toString())
        .map(Setting::getValue)
        .orElse(key.getDefaultValue());
  }

  /**
   * Persists a setting in the repository.
   *
   * @param key   The key of the setting to be persisted.
   * @param value The value to be associated with the setting.
   */
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
