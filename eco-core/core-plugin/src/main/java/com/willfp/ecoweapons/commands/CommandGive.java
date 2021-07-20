package com.willfp.ecoweapons.commands;

import com.willfp.eco.core.EcoPlugin;
import com.willfp.eco.core.command.CommandHandler;
import com.willfp.eco.core.command.TabCompleteHandler;
import com.willfp.eco.core.command.impl.Subcommand;
import com.willfp.eco.core.config.updating.ConfigUpdater;
import com.willfp.ecoweapons.sets.Weapon;
import com.willfp.ecoweapons.sets.Weapons;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandGive extends Subcommand {
    /**
     * The cached names.
     */
    private static final List<String> ITEM_NAMES = new ArrayList<>();

    /**
     * The cached numbers.
     */
    private static final List<String> NUMBERS = Arrays.asList(
            "1",
            "2",
            "3",
            "4",
            "5",
            "10",
            "32",
            "64"
    );

    /**
     * Instantiate a new command handler.
     *
     * @param plugin The plugin for the commands to listen for.
     */
    public CommandGive(@NotNull final EcoPlugin plugin) {
        super(plugin, "give", "ecoweapons.command.give", false);
        reload();
    }

    /**
     * Called on reload.
     */
    @ConfigUpdater
    public static void reload() {
        ITEM_NAMES.clear();
        ITEM_NAMES.addAll(Weapons.values().stream().map(Weapon::getName).collect(Collectors.toList()));
    }

    @Override
    public CommandHandler getHandler() {
        return (sender, args) -> {
            if (args.isEmpty()) {
                sender.sendMessage(this.getPlugin().getLangYml().getMessage("needs-player"));
                return;
            }

            if (args.size() == 1) {
                sender.sendMessage(this.getPlugin().getLangYml().getMessage("needs-item"));
                return;
            }

            String recieverName = args.get(0);
            Player reciever = Bukkit.getPlayer(recieverName);

            if (reciever == null) {
                sender.sendMessage(this.getPlugin().getLangYml().getMessage("invalid-player"));
                return;
            }

            String itemID = args.get(1);

            int amount = 1;

            Weapon weapon = Weapons.getByName(itemID.toLowerCase());

            if (weapon == null) {
                sender.sendMessage(this.getPlugin().getLangYml().getMessage("invalid-item"));
                return;
            }

            String message = this.getPlugin().getLangYml().getMessage("give-success");
            message = message.replace("%item%", weapon.getName()).replace("%recipient%", reciever.getName());
            sender.sendMessage(message);

            if (args.size() == 3) {
                try {
                    amount = Integer.parseInt(args.get(2));
                } catch (NumberFormatException ignored) {
                    // do nothing
                }
            }

            ItemStack item = weapon.getItem();
            item.setAmount(amount);
            reciever.getInventory().addItem(item);

        };
    }

    @Override
    public TabCompleteHandler getTabCompleter() {
        return (sender, args) -> {

            List<String> completions = new ArrayList<>();

            if (args.isEmpty()) {
                // Currently, this case is not ever reached
                return ITEM_NAMES;
            }

            if (args.size() == 1) {
                StringUtil.copyPartialMatches(args.get(0), Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), completions);
                return completions;
            }

            if (args.size() == 2) {
                StringUtil.copyPartialMatches(args.get(1), ITEM_NAMES, completions);

                Collections.sort(completions);
                return completions;
            }

            if (args.size() == 3) {
                StringUtil.copyPartialMatches(args.get(2), NUMBERS, completions);

                return completions;
            }

            return new ArrayList<>(0);
        };
    }
}
