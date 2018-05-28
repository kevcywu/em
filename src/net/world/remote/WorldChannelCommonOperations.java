package net.world.remote;

import java.rmi.RemoteException;

public interface WorldChannelCommonOperations {

    public boolean isConnected(String charName) throws RemoteException;

    public void broadcastMessage(String sender, byte[] message) throws RemoteException;

    public void whisper(String sender, String target, int channel, String message) throws RemoteException;

    public void shutdown(int time) throws RemoteException;

    public void loggedOn(String name, int characterId, int channel, int[] buddies) throws RemoteException;

    public void loggedOff(String name, int characterId, int channel, int[] buddies) throws RemoteException;

    public void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) throws RemoteException;
}
