package server.movement;

import tools.data.output.LittleEndianWriter;

public class ChangeEquipSpecialAwesome implements LifeMovementFragment {

    private int wui;

    public ChangeEquipSpecialAwesome(int wui) {
        this.wui = wui;
    }

    @Override
    public void serialize(LittleEndianWriter lew) {
        lew.write(10);
        lew.write(wui);
    }
}
