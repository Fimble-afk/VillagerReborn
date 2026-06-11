package com.villagerreborn.mod.villager;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

import java.util.*;

/**
 * Provides rich, contextual dialogue lines for villagers based on
 * their profession, time of day, player rank, and active quests.
 */
public class VillagerDialogue {

    private static final Random RANDOM = new Random();

    // ---- Greeting Lines by Rank ----
    public static final Map<Integer, List<String>> RANK_GREETINGS = new HashMap<>();
    static {
        RANK_GREETINGS.put(0, Arrays.asList(
            "Hmm... a traveler. State your business.",
            "I don't know you. What do you want?",
            "We don't often see outsiders here.",
            "You're not from around here, are you?",
            "What brings you to our village, stranger?"
        ));
        RANK_GREETINGS.put(1, Arrays.asList(
            "Oh, it's you! Good to see a familiar face.",
            "Ah, welcome back! The village is safer with you around.",
            "Hello there! Have you had a chance to speak with the elder?",
            "Good day! We appreciate the help you've given us.",
            "Welcome! The children have been asking about you."
        ));
        RANK_GREETINGS.put(2, Arrays.asList(
            "Friend! Come in, come in! We've been hoping you'd visit.",
            "Wonderful to see you, dear friend! You're always welcome here.",
            "The whole village talks about your deeds. You're a hero to us!",
            "My old friend! I've been saving something special for you.",
            "It warms my heart to see you. Please, stay as long as you like!"
        ));
        RANK_GREETINGS.put(3, Arrays.asList(
            "Our champion approaches! The village is honored by your presence.",
            "Great ally! Your reputation precedes you across all the lands.",
            "I heard of your latest deeds from the trading caravans. Remarkable!",
            "The village elder wishes to speak with you when you have a moment.",
            "Welcome back, trusted ally! The council has prepared a report for you."
        ));
        RANK_GREETINGS.put(4, Arrays.asList(
            "All hail our champion! The village owes you a debt we can never repay.",
            "Champion! Your legend is sung in every tavern from here to the coast.",
            "We are blessed to have you as our champion. The village is yours!",
            "The bell tolls in your honor today, great champion!",
            "Our champion returns! Let the whole village rejoice!"
        ));
    }

    // ---- Profession-specific chitchat ----
    public static final Map<VillagerProfession, List<String>> PROF_CHITCHAT = new HashMap<>();
    static {
        PROF_CHITCHAT.put(VillagerProfession.FARMER, Arrays.asList(
            "The harvest looks promising this year if the rains hold.",
            "I've been trying a new crop rotation — wheat, then carrots, then beets.",
            "Have you ever tried golden carrots? Best food I've ever eaten!",
            "The soil near the river is especially fertile.",
            "I worry about the crops when there's a full moon... strange things happen.",
            "My grandmother taught me to plant by the phases of the moon.",
            "A good farmer rises with the sun and sleeps when the chickens do!"
        ));
        PROF_CHITCHAT.put(VillagerProfession.LIBRARIAN, Arrays.asList(
            "I've been cataloguing ancient texts. Fascinating stuff.",
            "Did you know there are dungeons with entire libraries underground?",
            "Knowledge is the greatest treasure. No chest can hold it all!",
            "I've been researching enchantment theory. The results are... explosive.",
            "If you find any enchanted books on your travels, bring them to me!",
            "The ancient builders wrote in a language I still can't fully decipher.",
            "Reading by candlelight strains the eyes, but I can't stop!"
        ));
        PROF_CHITCHAT.put(VillagerProfession.WEAPONSMITH, Arrays.asList(
            "That sword of yours could use some sharpening. Just saying.",
            "The best steel comes from the deepest mines. I'd know.",
            "I once forged a blade so sharp it cut through diamond. Or so the legend goes.",
            "You can't put a price on good craftsmanship. Well, I can. 12 emeralds.",
            "My anvil has seen better days. The vibrations, you know.",
            "A well-balanced weapon makes all the difference in a fight.",
            "I've been experimenting with new alloys. Terrible accidents. Wonderful results."
        ));
        PROF_CHITCHAT.put(VillagerProfession.TOOLSMITH, Arrays.asList(
            "A good pickaxe is a miner's best friend. Second only to torches.",
            "I've been perfecting the geometry of the hoe. Revolutionary.",
            "Every tool I craft is guaranteed... mostly.",
            "The secret to a good shovel is in the handle angle.",
            "I once made a shovel so efficient I dug to bedrock in a day!"
        ));
        PROF_CHITCHAT.put(VillagerProfession.ARMORER, Arrays.asList(
            "That armor of yours won't stop a creeper blast. Trust me on this.",
            "I'm working on a new chestplate design. Lighter but stronger.",
            "Netherite? Yes, I've worked with it. Terrifying material.",
            "A good fit is everything in armor. No gaps for arrows!",
            "I've been reinforcing the watchtower walls. Just in case."
        ));
        PROF_CHITCHAT.put(VillagerProfession.CLERIC, Arrays.asList(
            "The spirits have been restless lately. I've been burning incense nonstop.",
            "Healing potions only go so far. Faith fills the rest.",
            "I sensed a darkness approaching from the east. Stay vigilant.",
            "The undead cannot stand in the light of our beacon. We're safe... for now.",
            "I've been brewing a new regeneration potion. The side effects are temporary."
        ));
        PROF_CHITCHAT.put(VillagerProfession.BUTCHER, Arrays.asList(
            "Nothing goes to waste in my shop. Nothing.",
            "I make the best mutton stew in three villages. Ask anyone!",
            "The pigs have been restless lately. Strange behavior.",
            "A hearty meal before battle — that's the true secret weapon.",
            "I traded some fine cuts to a wandering merchant. Should have kept them."
        ));
        PROF_CHITCHAT.put(VillagerProfession.LEATHERWORKER, Arrays.asList(
            "Leather armor is underrated. Flexible, lightweight, fashionable!",
            "I've been tanning hides all morning. My hands will smell for a week.",
            "A good saddle is worth its weight in gold. Ask any horse.",
            "I'm working on a new backpack design. Just need the right hide."
        ));
        PROF_CHITCHAT.put(VillagerProfession.FLETCHER, Arrays.asList(
            "I test every bow myself. My draw weight is legendary.",
            "The best arrows are made from bamboo shafts. Can't find bamboo here though.",
            "A quiver full of arrows means a night full of possibilities.",
            "I've been fletching crossbow bolts for the guards. Extra precaution."
        ));
        PROF_CHITCHAT.put(VillagerProfession.FISHERMAN, Arrays.asList(
            "The fish aren't biting today. Must be the weather.",
            "I caught something massive last week. Couldn't quite reel it in.",
            "There's something strange in the river. Glowing. I keep my distance.",
            "Best fishing spot? I'll tell you... but you didn't hear it from me."
        ));
        PROF_CHITCHAT.put(VillagerProfession.SHEPHERD, Arrays.asList(
            "My sheep produce the finest wool in the region. Secret diet.",
            "I lost a sheep to a wolf last week. The others are still spooked.",
            "Did you know sheep can recognize faces? Mine know mine.",
            "Colored wool is an art form. People don't appreciate it enough."
        ));
        PROF_CHITCHAT.put(VillagerProfession.MASON, Arrays.asList(
            "Every stone has a proper place. My job is finding it.",
            "I'm designing an expansion for the village. Three new buildings!",
            "The east wall needs reinforcing. I've been saying it for months.",
            "Smooth stone or cobblestone? It's a philosophical question, really.",
            "I built the well and the fountain. The well came out better."
        ));
        PROF_CHITCHAT.put(VillagerProfession.CARTOGRAPHER, Arrays.asList(
            "I've mapped most of the region, but there's still a blank spot to the north.",
            "A good map is worth more than gold in unmapped territory.",
            "I've heard of a woodland mansion three days' journey east. Fascinating.",
            "The coordinates I use are completely accurate. Mostly.",
            "I once got lost using my own map. Revised edition coming soon."
        ));
        // Default for unspecified professions
        PROF_CHITCHAT.put(VillagerProfession.NONE, Arrays.asList(
            "Life in the village is peaceful. Mostly.",
            "Have you explored the caves nearby? They say there's treasure.",
            "The nights have been getting darker. More torches, I say!",
            "I've been thinking about expanding my home. Need more rooms.",
            "Have you heard the rumors about the abandoned mineshaft to the west?"
        ));
    }

    // ---- Night time lines ----
    public static final List<String> NIGHT_LINES = Arrays.asList(
        "Please be careful out there! The undead roam at night.",
        "Stay indoors after dark, traveler. It's not safe.",
        "I should be getting inside. The zombies... they frighten me.",
        "Our iron golem patrols at night, but sometimes it's not enough.",
        "Lock your doors tonight. I heard groaning from the forest.",
        "Can you hear that? Something is out there in the dark."
    );

    // ---- Quest offer lines ----
    public static final List<String> QUEST_OFFER = Arrays.asList(
        "Actually, I could use your help with something...",
        "Say, since you're here — could you do me a favor?",
        "Ah, perfect timing! I have a task that needs doing.",
        "You look capable. I have a job that might interest you.",
        "I've been hoping someone like you would come along. I need help!"
    );

    // ---- Quest completion lines ----
    public static final List<String> QUEST_COMPLETE = Arrays.asList(
        "Wonderful! You've done it! The whole village thanks you!",
        "Incredible! I knew I could count on you!",
        "You're truly a blessing to our village. Here, take this as thanks.",
        "Well done! Your reputation here grows with each deed.",
        "This is fantastic news! I'll tell the others right away!"
    );

    // ---- Quest reminder lines ----
    public static final List<String> QUEST_REMINDER = Arrays.asList(
        "Still working on that task I gave you?",
        "No rush, but... have you made progress on my request?",
        "I'm still waiting on that errand. The village is counting on you.",
        "Any luck with what I asked?"
    );

    // ---- Village expansion lines (when building) ----
    public static final List<String> EXPANSION_LINES = Arrays.asList(
        "We're expanding the village! Exciting times ahead.",
        "Have you seen our new construction? The village is growing!",
        "More buildings means more neighbors. I'm thrilled... mostly.",
        "The mason has been working night and day on the new district.",
        "We're building a new market square. Trade will flourish!"
    );

    // ---- Rank-up congratulation lines ----
    public static final Map<Integer, String> RANK_UP_MESSAGES = new HashMap<>();
    static {
        RANK_UP_MESSAGES.put(1, "§aYou are now an §lAcquaintance§r§a of this village! The villagers recognize your face.");
        RANK_UP_MESSAGES.put(2, "§2You are now a §lFriend§r§2 of this village! You are always welcome here.");
        RANK_UP_MESSAGES.put(3, "§6You are now an §lAlly§r§6 of this village! The village will stand with you.");
        RANK_UP_MESSAGES.put(4, "§5You are now a §lChampion§r§5 of this village! Your name will be remembered forever.");
    }

    // ---- Utility methods ----

    public static String getGreeting(int rank, VillagerProfession prof, boolean isNight) {
        if (isNight) {
            return NIGHT_LINES.get(RANDOM.nextInt(NIGHT_LINES.size()));
        }
        List<String> greetings = RANK_GREETINGS.getOrDefault(rank, RANK_GREETINGS.get(0));
        return greetings.get(RANDOM.nextInt(greetings.size()));
    }

    public static String getProfessionChitchat(VillagerProfession prof) {
        List<String> lines = PROF_CHITCHAT.getOrDefault(prof, PROF_CHITCHAT.get(VillagerProfession.NONE));
        return lines.get(RANDOM.nextInt(lines.size()));
    }

    public static String getQuestOffer() {
        return QUEST_OFFER.get(RANDOM.nextInt(QUEST_OFFER.size()));
    }

    public static String getQuestComplete() {
        return QUEST_COMPLETE.get(RANDOM.nextInt(QUEST_COMPLETE.size()));
    }

    public static String getQuestReminder() {
        return QUEST_REMINDER.get(RANDOM.nextInt(QUEST_REMINDER.size()));
    }

    public static String getExpansionLine() {
        return EXPANSION_LINES.get(RANDOM.nextInt(EXPANSION_LINES.size()));
    }

    /**
     * Format a quest description for display in chat.
     */
    public static String formatQuestMessage(com.villagerreborn.mod.quest.QuestManager.QuestDefinition quest, int progress) {
        StringBuilder sb = new StringBuilder();
        sb.append("§e[Quest: ").append(quest.title).append("]§r\n");
        sb.append("§7").append(quest.description).append("\n");
        if (quest.type == com.villagerreborn.mod.quest.QuestManager.QuestType.KILL_MOBS) {
            sb.append("§fProgress: ").append(progress).append("/").append(quest.targetCount);
            sb.append(" ").append(quest.targetMob.replace("minecraft:", "")).append("(s) slain\n");
        } else {
            sb.append("§fBring: ").append(quest.targetCount).append("x ")
              .append(quest.targetItem.replace("minecraft:", "")).append("\n");
            if (progress > 0) {
                sb.append("§fIn inventory: ").append(progress).append("/").append(quest.targetCount).append("\n");
            }
        }
        sb.append("§aReward: ").append(quest.emeraldReward).append(" emerald(s) + ")
          .append(quest.relationReward).append(" relation");
        return sb.toString();
    }
}
