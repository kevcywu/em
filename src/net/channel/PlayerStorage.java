package net.channel;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import client.MapleCharacter;

public class PlayerStorage implements IPlayerStorage {

    Map<String, MapleCharacter> nameToChar = new LinkedHashMap<>();
    Map<Integer, MapleCharacter> idToChar = new LinkedHashMap<>();

    public void registerPlayer(MapleCharacter chr) {
        nameToChar.put(chr.getName().toLowerCase(), chr);
        idToChar.put(chr.getId(), chr);
    }

    public void deregisterPlayer(MapleCharacter chr) {
        nameToChar.remove(chr.getName().toLowerCase());
        idToChar.remove(chr.getId());
    }

    @Override
    public MapleCharacter getCharacterByName(String name) {
        return nameToChar.get(name.toLowerCase());
    }

    @Override
    public MapleCharacter getCharacterById(int id) {
        return idToChar.get(id);
    }

    @Override
    public Collection<MapleCharacter> getAllCharacters() {
        return nameToChar.values();
    }
}
