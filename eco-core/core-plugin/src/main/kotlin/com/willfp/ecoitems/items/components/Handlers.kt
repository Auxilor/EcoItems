package com.willfp.ecoitems.items.components

import com.willfp.ecoitems.compat.ModernCompatibilityProxy

@ModernCompatibilityProxy("components.ToolComponentHandlerImpl")
abstract class ToolComponentHandler: ComponentHandler("tool")

@ModernCompatibilityProxy("components.FoodComponentHandlerImpl")
abstract class FoodComponentHandler: ComponentHandler("food")
