package server.quest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Properties;

import client.MapleQuestStatus;
import database.DatabaseConnection;

public class MapleCustomQuest extends MapleQuest {

    public MapleCustomQuest(int id) {
        try {
            this.id = id;
            startActs = new LinkedList<MapleQuestAction>();
            completeActs = new LinkedList<MapleQuestAction>();
            startReqs = new LinkedList<MapleQuestRequirement>();
            completeReqs = new LinkedList<MapleQuestRequirement>();
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM questrequirements WHERE "
                    + "questid = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            MapleQuestRequirement req;
            MapleCustomQuestData data;
            while (rs.next()) {
                Blob blob = rs.getBlob("data");
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
                        blob.getBytes(1, (int) blob.length())));
                data = (MapleCustomQuestData) ois.readObject();
                req = new MapleQuestRequirement(this,
                        MapleQuestRequirementType.getByWZName(data.getName()), data);
                MapleQuestStatus.Status status = MapleQuestStatus.Status.getById(
                        rs.getInt("status"));
                if (status.equals(MapleQuestStatus.Status.NOT_STARTED)) {
                    startReqs.add(req);
                } else if (status.equals(MapleQuestStatus.Status.STARTED)) {
                    completeReqs.add(req);
                }
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT * FROM questactions WHERE questid = ?");
            ps.setInt(1, id);
            rs = ps.executeQuery();
            MapleQuestAction act;
            while (rs.next()) {
                Blob blob = rs.getBlob("data");
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
                        blob.getBytes(1, (int) blob.length())));
                data = (MapleCustomQuestData) ois.readObject();
                act = new MapleQuestAction(MapleQuestActionType.getByWZName(data.getName()), data, this);
                MapleQuestStatus.Status status = MapleQuestStatus.Status.getById(
                        rs.getInt("status"));
                if (status.equals(MapleQuestStatus.Status.NOT_STARTED)) {
                    startActs.add(act);
                } else if (status.equals(MapleQuestStatus.Status.STARTED)) {
                    completeActs.add(act);
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            log.error("Error loading custom quest.", ex);
        } catch (IOException e) {
            log.error("Error loading custom quest.", e);
        } catch (ClassNotFoundException e) {
            log.error("Error loading custom quest.", e);
        }
    }

    public static void main(String[] args) throws Exception {
        // 3rd job
        int questid = 100100;

        Connection con = DatabaseConnection.getConnection();
        PreparedStatement psr = con.prepareStatement("INSERT INTO questrequirements VALUES (DEFAULT, ?, ?, ?)");
        PreparedStatement psa = con.prepareStatement("INSERT INTO questactions VALUES (DEFAULT, ?, ?, ?)");
        psr.setInt(1, questid);
        psa.setInt(1, questid);
        MapleCustomQuestData data;
        MapleCustomQuestData dataEntry;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        data = new MapleCustomQuestData("lvmin", 70, null);
        psr.setInt(2, MapleQuestStatus.Status.NOT_STARTED.getId());
        bos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(bos);
        oos.writeObject(data);
        oos.flush();
        psr.setBlob(3, new ByteArrayInputStream(bos.toByteArray()));
        psr.executeUpdate();

        data = new MapleCustomQuestData("item", null, null);
        dataEntry = new MapleCustomQuestData("0", null, data);
        data.addChild(dataEntry);
        dataEntry.addChild(new MapleCustomQuestData("id", 4031057, dataEntry));
        dataEntry.addChild(new MapleCustomQuestData("count", 1, dataEntry));
        psr.setInt(2, MapleQuestStatus.Status.STARTED.getId());
        bos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(bos);
        oos.writeObject(data);
        oos.flush();
        psr.setBlob(3, new ByteArrayInputStream(bos.toByteArray()));
        psr.executeUpdate();

        data = new MapleCustomQuestData("item", null, null);
        dataEntry = new MapleCustomQuestData("1", null, data);
        data.addChild(dataEntry);
        dataEntry.addChild(new MapleCustomQuestData("id", 4031057, dataEntry));
        dataEntry.addChild(new MapleCustomQuestData("count", -1, dataEntry));
        psa.setInt(2, MapleQuestStatus.Status.STARTED.getId());
        bos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(bos);
        oos.writeObject(data);
        oos.flush();
        psa.setBlob(3, new ByteArrayInputStream(bos.toByteArray()));
        psa.executeUpdate();

        // clone quest (still 3rd job)
        questid = 100101;

        psr.setInt(1, questid);
        psa.setInt(1, questid);

        data = new MapleCustomQuestData("quest", null, null);
        dataEntry = new MapleCustomQuestData("0", null, data);
        data.addChild(dataEntry);
        dataEntry.addChild(new MapleCustomQuestData("id", 100100, dataEntry));
        dataEntry.addChild(new MapleCustomQuestData("state", MapleQuestStatus.Status.STARTED.getId(), dataEntry));
        psr.setInt(2, MapleQuestStatus.Status.NOT_STARTED.getId());
        bos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(bos);
        oos.writeObject(data);
        oos.flush();
        psr.setBlob(3, new ByteArrayInputStream(bos.toByteArray()));
        psr.executeUpdate();

        data = new MapleCustomQuestData("item", null, null);
        dataEntry = new MapleCustomQuestData("0", null, data);
        data.addChild(dataEntry);
        dataEntry.addChild(new MapleCustomQuestData("id", 4031059, dataEntry));
        dataEntry.addChild(new MapleCustomQuestData("count", 1, dataEntry));
        psr.setInt(2, MapleQuestStatus.Status.STARTED.getId());
        bos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(bos);
        oos.writeObject(data);
        oos.flush();
        psr.setBlob(3, new ByteArrayInputStream(bos.toByteArray()));
        psr.executeUpdate();

        data = new MapleCustomQuestData("item", null, null);
        dataEntry = new MapleCustomQuestData("0", null, null);
        data.addChild(dataEntry);
        dataEntry.addChild(new MapleCustomQuestData("id", 4031059, dataEntry));
        dataEntry.addChild(new MapleCustomQuestData("count", -1, dataEntry));
        dataEntry = new MapleCustomQuestData("1", null, data);
        data.addChild(dataEntry);
        dataEntry.addChild(new MapleCustomQuestData("id", 4031057, dataEntry));
        dataEntry.addChild(new MapleCustomQuestData("count", 1, dataEntry));
        psa.setInt(2, MapleQuestStatus.Status.STARTED.getId());
        bos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(bos);
        oos.writeObject(data);
        oos.flush();
        psa.setBlob(3, new ByteArrayInputStream(bos.toByteArray()));
        psa.executeUpdate();

        // quiz quest (still 3rd job)
        questid = 100102;

        psr.setInt(1, questid);
        psa.setInt(1, questid);

        data = new MapleCustomQuestData("lvmin", 70, null);
        psr.setInt(2, MapleQuestStatus.Status.NOT_STARTED.getId());
        bos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(bos);
        oos.writeObject(data);
        oos.flush();
        psr.setBlob(3, new ByteArrayInputStream(bos.toByteArray()));
        psr.executeUpdate();

        data = new MapleCustomQuestData("item", null, null);
        dataEntry = new MapleCustomQuestData("0", null, data);
        data.addChild(dataEntry);
        dataEntry.addChild(new MapleCustomQuestData("id", 4031058, dataEntry));
        dataEntry.addChild(new MapleCustomQuestData("count", 1, dataEntry));
        psr.setInt(2, MapleQuestStatus.Status.STARTED.getId());
        bos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(bos);
        oos.writeObject(data);
        oos.flush();
        psr.setBlob(3, new ByteArrayInputStream(bos.toByteArray()));
        psr.executeUpdate();

        data = new MapleCustomQuestData("item", null, null);
        dataEntry = new MapleCustomQuestData("1", null, data);
        data.addChild(dataEntry);
        dataEntry.addChild(new MapleCustomQuestData("id", 4031058, dataEntry));
        dataEntry.addChild(new MapleCustomQuestData("count", -1, dataEntry));
        psa.setInt(2, MapleQuestStatus.Status.STARTED.getId());
        bos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(bos);
        oos.writeObject(data);
        oos.flush();
        psa.setBlob(3, new ByteArrayInputStream(bos.toByteArray()));
        psa.executeUpdate();

        psr.close();
        psa.close();
        con.close();

    }

}
