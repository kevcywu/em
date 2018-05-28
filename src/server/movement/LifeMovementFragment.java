package server.movement;

import tools.data.output.LittleEndianWriter;

public interface LifeMovementFragment {

    void serialize(LittleEndianWriter lew);
}
