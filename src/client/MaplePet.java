package client;

public class MaplePet extends Item {

    private String name;

    public MaplePet(int id, byte position) {
        super(id, position, (short) 1);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
