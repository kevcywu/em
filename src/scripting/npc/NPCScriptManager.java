package scripting.npc;

import java.util.HashMap;
import java.util.Map;
import javax.script.Invocable;
import client.MapleClient;
import scripting.AbstractScriptManager;

public class NPCScriptManager extends AbstractScriptManager {

    private final Map<MapleClient, NPCConversationManager> cms = new HashMap<>();
    private final Map<MapleClient, NPCScript> scripts = new HashMap<MapleClient, NPCScript>();
    private static final NPCScriptManager instance = new NPCScriptManager();

    public synchronized static NPCScriptManager getInstance() {
        return instance;
    }

    public void start(MapleClient c, int npc) {
        try {
            NPCConversationManager cm = new NPCConversationManager(c, npc);
            if (cms.containsKey(c)) {
                return;
            }
            cms.put(c, cm);
            Invocable iv = getInvocable("npc/" + npc + ".js", c);
            if (iv == null) {
                cm.dispose();
                return;
            }
            engine.put("cm", cm);
            NPCScript ns = iv.getInterface(NPCScript.class);
            scripts.put(c, ns);
            ns.start();
        } catch (Exception e) {
            log.error("Error executing NPC script.", e);
            dispose(c);
            cms.remove(c);
        }
    }

    public void action(MapleClient c, byte mode, byte type, byte selection) {
        NPCScript ns = scripts.get(c);
        if (ns != null) {
            try {
                ns.action(mode, type, selection);
            } catch (Exception e) {
                log.error("Error executing NPC script.", e);
                dispose(c);
            }
        }
    }

    public void dispose(NPCConversationManager cm) {
        cms.remove(cm.getC());
        scripts.remove(cm.getC());
        resetContext("npc/" + cm.getNpc() + ".js", cm.getC());
    }

    public void dispose(MapleClient c) {
        NPCConversationManager npccm = cms.get(c);
        if (npccm != null) {
            dispose(npccm);
        }
    }

    public NPCConversationManager getCM(MapleClient c) {
        return cms.get(c);
    }
}
