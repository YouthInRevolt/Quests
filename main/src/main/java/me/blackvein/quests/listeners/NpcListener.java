/*******************************************************************************************************
 * Continued by PikaMug (formerly HappyPikachu) with permission from _Blackvein_. All rights reserved.
 * 
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
 * NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************************************/

package me.blackvein.quests.listeners;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.Conversation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import me.blackvein.quests.Quest;
import me.blackvein.quests.Quester;
import me.blackvein.quests.Quests;
import me.blackvein.quests.util.ItemUtil;
import me.blackvein.quests.util.Lang;
import me.blackvein.quests.util.MiscUtil;
import me.blackvein.quests.util.RomanNumeral;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;

public class NpcListener implements Listener {

    private final Quests plugin;

    public NpcListener(Quests newPlugin) {
        plugin = newPlugin;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onNPCRightClick(NPCRightClickEvent evt) {
        if (plugin.getQuestFactory().getSelectingNpcs().contains(evt.getClicker().getUniqueId())) {
            evt.getClicker().sendMessage(ChatColor.GREEN + evt.getNPC().getName() + ": " + ChatColor.DARK_GREEN + "ID "
                    + evt.getNPC().getId());
            return;
        }
        if (evt.getClicker().isConversing() == false) {
            final Player player = evt.getClicker();
            final Quester quester = plugin.getQuester(player.getUniqueId());
            boolean delivery = false;
            for (Quest quest : quester.getCurrentQuests().keySet()) {
                if (quester.getCurrentStage(quest).containsObjective("deliverItem") && player.getItemInHand() != null) {
                    ItemStack hand = player.getItemInHand();
                    int currentIndex = -1;
                    LinkedList<Integer> matches = new LinkedList<Integer>();
                    int reasonCode = 0;
                    for (ItemStack is : quester.getCurrentStage(quest).getItemsToDeliver()) {
                        currentIndex++;
                        reasonCode = ItemUtil.compareItems(is, hand, true);
                        if (reasonCode == 0) {
                            matches.add(currentIndex);
                        }
                    }
                    NPC clicked = evt.getNPC();
                    if (!matches.isEmpty()) {
                        for (Integer match : matches) {
                            Integer id = quester.getCurrentStage(quest).getItemDeliveryTargets().get(match);
                            if (id.equals(clicked.getId())) {
                                quester.deliverToNPC(quest, clicked, hand);
                                delivery = true;
                                return;
                            }
                        }
                    } else if (!hand.getType().equals(Material.AIR)) {
                        for (Integer n : quester.getCurrentStage(quest).getItemDeliveryTargets()) {
                            if (n.equals(clicked.getId())) {
                                String text = "";
                                boolean hasMeta = hand.hasItemMeta();
                                if (hasMeta) {
                                    text += ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC
                                            + (hand.getItemMeta().hasDisplayName() ? hand.getItemMeta().getDisplayName()
                                            + ChatColor.GRAY + " (" : "");
                                }
                                text += ChatColor.AQUA + "<item>" + (hand.getDurability() != 0 ? (":" + ChatColor.BLUE 
                                        + hand.getDurability()) : "") + ChatColor.GRAY;
                                if (hasMeta) {
                                    text += (hand.getItemMeta().hasDisplayName() ? ")" : "");
                                }
                                text += " x " + ChatColor.DARK_AQUA + hand.getAmount() + ChatColor.GRAY;
                                if (plugin.getSettings().canTranslateNames() && !hasMeta 
                                        && !hand.getItemMeta().hasDisplayName()) {
                                    plugin.getLocaleQuery().sendMessage(player, Lang
                                            .get(player, "questInvalidDeliveryItem").replace("<item>", text), hand
                                            .getType(), hand.getDurability(), null);
                                } else {
                                    player.sendMessage(Lang.get(player, "questInvalidDeliveryItem")
                                            .replace("<item>", text).replace("<item>", ItemUtil.getName(hand)));
                                }
                                switch (reasonCode) {
                                    case 1:
                                        player.sendMessage(ChatColor.GRAY + Lang.get(player, "difference")
                                                .replace("<data>", "one item is null"));
                                        break;
                                    case 0:
                                        // Should never happen
                                        player.sendMessage(ChatColor.GRAY + Lang.get(player, "difference")
                                                .replace("<data>", "ERROR"));
                                        break;
                                    case -1:
                                        player.sendMessage(ChatColor.GRAY + Lang.get(player, "difference")
                                                .replace("<data>", "name"));
                                        break;
                                    case -2:
                                        player.sendMessage(ChatColor.GRAY + Lang.get(player, "difference")
                                                .replace("<data>", "amount"));
                                        break;
                                    case -3:
                                        player.sendMessage(ChatColor.GRAY + Lang.get(player, "difference")
                                                .replace("<data>", "durability"));
                                        break;
                                    case -4:
                                        player.sendMessage(ChatColor.GRAY + Lang.get(player, "difference")
                                                .replace("<data>", "display name or lore"));
                                        break;
                                    case -5:
                                        player.sendMessage(ChatColor.GRAY + Lang.get(player, "difference")
                                                .replace("<data>", "enchantments"));
                                        break;
                                    case -6:
                                        player.sendMessage(ChatColor.GRAY + Lang.get(player, "difference")
                                                .replace("<data>", "stored enchants"));
                                        break;
                                    case -7:
                                        player.sendMessage(ChatColor.GRAY + Lang.get(player, "difference")
                                                .replace("<data>", "item flags"));
                                        break;
                                    case -8:
                                        player.sendMessage(ChatColor.GRAY + Lang.get(player, "difference")
                                                .replace("<data>", "book data"));
                                        break;
                                    case -9:
                                        player.sendMessage(ChatColor.GRAY + Lang.get(player, "difference")
                                                .replace("<data>", "potion type"));
                                        break;
                                    default:
                                        player.sendMessage(ChatColor.GRAY + Lang.get(player, "difference")
                                                .replace("<data>", "unknown"));
                                }
                                if (hasMeta) {
                                    if (hand.getType().equals(Material.ENCHANTED_BOOK)) {
                                        EnchantmentStorageMeta esmeta = (EnchantmentStorageMeta) hand.getItemMeta();
                                        if (esmeta.hasStoredEnchants()) {
                                            // TODO translate Roman numerals
                                            for (Entry<Enchantment, Integer> e : esmeta.getStoredEnchants()
                                                    .entrySet()) {
                                                HashMap<Enchantment, Integer> single 
                                                        = new HashMap<Enchantment, Integer>();
                                                single.put(e.getKey(), e.getValue());
                                                plugin.getLocaleQuery().sendMessage(player, ChatColor.GRAY + "\u2515 " 
                                                        + ChatColor.DARK_GREEN 
                                                        + "<enchantment> " + RomanNumeral.getNumeral(e.getValue()) 
                                                        + "\n", single);
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
            if (plugin.getQuestNpcs().contains(evt.getNPC()) && delivery == false) {
                boolean hasObjective = false;
                for (Quest quest : quester.getCurrentQuests().keySet()) {
                    if (quester.getCurrentStage(quest).containsObjective("talkToNPC")) {
                        if (quester.getQuestData(quest) != null 
                                && quester.getQuestData(quest).citizensInteracted.containsKey(evt.getNPC().getId()) 
                                && quester.getQuestData(quest).citizensInteracted.get(evt.getNPC().getId()) == false) {
                            hasObjective = true;
                        }
                        quester.interactWithNPC(quest, evt.getNPC());
                    }
                }
                if (!hasObjective) {
                    boolean hasAtLeastOneGUI = false;
                    LinkedList<Quest> npcQuests = new LinkedList<Quest>();
                    for (Quest q : plugin.getQuests()) {
                        if (quester.getCurrentQuests().containsKey(q))
                            continue;
                        if (q.getNpcStart() != null && q.getNpcStart().getId() == evt.getNPC().getId()) {
                            if (plugin.getSettings().canIgnoreLockedQuests() 
                                    && (quester.getCompletedQuests().contains(q.getName()) == false 
                                    || q.getPlanner().getCooldown() > -1)) {
                                if (q.testRequirements(quester)) {
                                    npcQuests.add(q);
                                    if (q.getGUIDisplay() != null) {
                                        hasAtLeastOneGUI = true;
                                    }
                                }
                            } else if (quester.getCompletedQuests().contains(q.getName()) == false 
                                    || q.getPlanner().getCooldown() > -1) {
                                npcQuests.add(q);
                                if (q.getGUIDisplay() != null) {
                                    hasAtLeastOneGUI = true;
                                }
                            }
                        }
                    }
                    if (npcQuests.isEmpty() == false && npcQuests.size() == 1) {
                        Quest q = npcQuests.get(0);
                        if (!quester.getCompletedQuests().contains(q.getName())) {
                            if (quester.getCurrentQuests().size() < plugin.getSettings().getMaxQuests() 
                                    || plugin.getSettings().getMaxQuests() < 1) {
                                quester.setQuestToTake(q.getName());
                                if (!plugin.getSettings().canAskConfirmation()) {
                                    quester.takeQuest(q, false);
                                } else {
                                    if (q.getGUIDisplay() != null) {
                                        quester.showGUIDisplay(evt.getNPC(), npcQuests);
                                    } else {
                                        for (String msg : extracted(quester).split("<br>")) {
                                            player.sendMessage(msg);
                                        }
                                        plugin.getConversationFactory().buildConversation((Conversable) player).begin();
                                    }
                                }
                            } else if (quester.getCurrentQuests().containsKey(q) == false) {
                                String msg = Lang.get(player, "questMaxAllowed");
                                msg = msg.replace("<number>", String.valueOf(plugin.getSettings().getMaxQuests()));
                                player.sendMessage(ChatColor.YELLOW + msg);
                            }
                        } else if (quester.getCurrentQuests().size() < plugin.getSettings().getMaxQuests() 
                                || plugin.getSettings().getMaxQuests() < 1) {
                            if (quester.getCooldownDifference(q) > 0) {
                                String early = Lang.get(player, "questTooEarly");
                                early = early.replace("<quest>", ChatColor.AQUA + q.getName() + ChatColor.YELLOW);
                                early = early.replace("<time>", ChatColor.DARK_PURPLE 
                                        + MiscUtil.getTime(quester.getCooldownDifference(q)) + ChatColor.YELLOW);
                                player.sendMessage(ChatColor.YELLOW + early);
                            } else if (q.getPlanner().getCooldown() < 0) {
                                String completed = Lang.get(player, "questAlreadyCompleted");
                                completed = completed.replace("<quest>", ChatColor.AQUA + q.getName() 
                                        + ChatColor.YELLOW);
                                player.sendMessage(ChatColor.YELLOW + completed);
                            } else {
                                quester.setQuestToTake(q.getName());
                                if (!plugin.getSettings().canAskConfirmation()) {
                                    quester.takeQuest(q, false);
                                } else {
                                    if (q.getGUIDisplay() != null) {
                                        quester.showGUIDisplay(evt.getNPC(), npcQuests);
                                    } else {
                                        for (String msg : extracted(quester).split("<br>")) {
                                            player.sendMessage(msg);
                                        }
                                        plugin.getConversationFactory().buildConversation((Conversable) player).begin();
                                    }
                                }
                            }
                        } else if (quester.getCurrentQuests().containsKey(q) == false) {
                            String msg = Lang.get(player, "questMaxAllowed");
                            msg = msg.replace("<number>", String.valueOf(plugin.getSettings().getMaxQuests()));
                            player.sendMessage(ChatColor.YELLOW + msg);
                        }
                    } else if (npcQuests.isEmpty() == false && npcQuests.size() > 1) {
                            if (hasAtLeastOneGUI) {
                                quester.showGUIDisplay(evt.getNPC(), npcQuests);
                            } else {
                                Conversation c = plugin.getNpcConversationFactory().buildConversation(player);
                                c.getContext().setSessionData("quests", npcQuests);
                                c.getContext().setSessionData("npc", evt.getNPC().getName());
                                c.begin();
                            }
                            return;
                    } else if (npcQuests.isEmpty()) {
                        evt.getClicker().sendMessage(ChatColor.YELLOW + Lang.get(player, "noMoreQuest"));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onNPCLeftClick(NPCLeftClickEvent evt) {
        if (plugin.getQuestFactory().getSelectingNpcs().contains(evt.getClicker().getUniqueId())) {
            evt.getClicker().sendMessage(ChatColor.GREEN + evt.getNPC().getName() + ": " + ChatColor.DARK_GREEN 
                    + Lang.get("id") + " " + evt.getNPC().getId());
        }
    }

    @EventHandler
    public void onNPCDeath(NPCDeathEvent evt) {
        if (evt.getNPC() == null || evt.getNPC().getEntity() == null 
                || evt.getNPC().getEntity().getLastDamageCause() == null) {
            return;
        }
        if (evt.getNPC().getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent 
                    = (EntityDamageByEntityEvent) evt.getNPC().getEntity().getLastDamageCause();
            Entity damager = damageEvent.getDamager();
            if (damager != null) {
                if (damager instanceof Projectile) {
                    if (evt.getNPC().getEntity().getLastDamageCause().getEntity() instanceof Player) {
                        Player player = (Player) evt.getNPC().getEntity().getLastDamageCause().getEntity();
                        boolean okay = true;
                        if (plugin.getDependencies().getCitizens() != null) {
                            if (CitizensAPI.getNPCRegistry().isNPC(player)) {
                                okay = false;
                            }
                        }
                        if (okay) {
                            Quester quester = plugin.getQuester(player.getUniqueId());
                            for (Quest quest : quester.getCurrentQuests().keySet()) {
                                if (quester.getCurrentStage(quest).containsObjective("killNPC")) {
                                    quester.killNPC(quest, evt.getNPC());
                                }
                            }
                        }
                    }
                } else if (damager instanceof Player) {
                    boolean okay = true;
                    if (plugin.getDependencies().getCitizens() != null) {
                        if (plugin.getDependencies().getCitizens().getNPCRegistry().isNPC(damager)) {
                            okay = false;
                        }
                    }
                    if (okay) {
                        Player player = (Player) damager;
                        Quester quester = plugin.getQuester(player.getUniqueId());
                        for (Quest quest : quester.getCurrentQuests().keySet()) {
                            if (quester.getCurrentStage(quest).containsObjective("killNPC")) {
                                quester.killNPC(quest, evt.getNPC());
                            }
                        }
                    }
                }
            }
        }
    }

    private String extracted(final Quester quester) {
        return MessageFormat.format("{0}- {1}{2}{3} -\n\n{4}{5}\n", ChatColor.GOLD, ChatColor.DARK_PURPLE, 
                quester.getQuestToTake(), ChatColor.GOLD, ChatColor.RESET, plugin.getQuest(quester.getQuestToTake())
                .getDescription());
    }
}
