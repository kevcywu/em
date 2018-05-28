package scripting.npc;

public interface NPCScript {

    public void start();

    public void action(byte mode, byte type, byte selection);

}
