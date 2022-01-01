package com.willfp.ecoitems.config

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.config.BaseConfig
import com.willfp.eco.core.config.ConfigType

class ItemsYml(plugin: EcoPlugin) : BaseConfig("items", plugin, false, ConfigType.YAML)