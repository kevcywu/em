package net.channel.handler;

import client.ISkill;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleStat;
import client.SkillFactory;
import net.AbstractMaplePacketHandler;
import tools.data.input.SeekableLittleEndianAccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributeSPHandler extends AbstractMaplePacketHandler {

    private static final Logger log = LoggerFactory.getLogger(DistributeSPHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        slea.readInt(); // something strange
        int skillid = slea.readInt();
        boolean isBegginnerSkill = false;

        MapleCharacter player = c.getPlayer();
        int remainingSp = player.getRemainingSp();
        if (skillid == 1000 || skillid == 1001 || skillid == 1002) { // boo beginner skill
            int snailsLevel = player.getSkillLevel(SkillFactory.getSkill(1000));
            int recoveryLevel = player.getSkillLevel(SkillFactory.getSkill(1001));
            int nimbleFeetLevel = player.getSkillLevel(SkillFactory.getSkill(1002));
            remainingSp = Math.min((player.getLevel() - 1), 6) - snailsLevel - recoveryLevel - nimbleFeetLevel;
            isBegginnerSkill = true;
        }
        ISkill skill = SkillFactory.getSkill(skillid);
        int maxlevel = skill.getMaxLevel();
        int curLevel = player.getSkillLevel(skill);
        if ((remainingSp > 0 && curLevel + 1 <= maxlevel) && skill.canBeLearnedBy(player.getJob())) {
            if (!isBegginnerSkill) {
                player.setRemainingSp(player.getRemainingSp() - 1);
            }
            player.updateSingleStat(MapleStat.AVAILABLESP, player.getRemainingSp());
            player.changeSkillLevel(skill, curLevel + 1, player.getMasterLevel(skill));
        } else if (!skill.canBeLearnedBy(player.getJob())) {
            return;
//   AutobanManager.getInstance().addPoints(c, 1000, 0, "Trying to learn a skill for a different job (" + player.getJob().name() + ":" + skillid + ")");
        } else if (!(remainingSp > 0 && curLevel + 1 <= maxlevel)) {
            //AutobanManager.getInstance().addPoints(c, 334, 120000, "Trying to distribute SP to " + skillid + " without having any");
            log.info("[h4x] Player {} is distributing SP to {} without having any", player.getName(), Integer.valueOf(skillid));
        }
    }
}
