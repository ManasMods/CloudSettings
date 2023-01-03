package com.github.manasmods.cloudsettings.cloudservice.response;

import com.github.manasmods.cloudsettings.cloudservice.pojo.Setting;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PlayerSettingsResponse {
    @Getter
    private DefaultResponse result;
    @Getter
    private List<Setting> entries;
}
