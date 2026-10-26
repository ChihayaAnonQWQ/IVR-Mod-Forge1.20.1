package net.hulan.ksd.data;

import io.netty.buffer.Unpooled;
import mtr.data.MessagePackHelper;
import mtr.data.Station;
import mtr.data.TransportMode;
import net.hulan.Tuples;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Tuple;
import org.msgpack.core.MessagePacker;
import org.msgpack.value.Value;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class KSDStation extends KSDAreaBase {

    public int zone;
    public final Map<String, List<String>> exits = new HashMap<>();
    private static final String KEY_ZONE = "zone";
    private static final String KEY_EXITS = "exits";
    private static final String KEY_EXIT_EDIT_PARENT = "exit_edit_parent";
    private static final String KEY_EXIT_DELETE_PARENT = "exit_delete_parent";
    private static final String KEY_EXIT_DESTINATIONS = "exit_destinations";

    public static KSDStation fromMTRStation(Station mtrStation) {
        long id = mtrStation.id;
        TransportMode transportMode = mtrStation.transportMode;
        KSDStation newStation = new KSDStation(id, transportMode);
        newStation.zone = mtrStation.zone;
        newStation.exits.putAll(mtrStation.exits);
        newStation.corner1 = mtrStation.corner1 == null ? new Tuples<>(0, -64, 0) : Tuples.fromTuple(mtrStation.corner1);
        newStation.corner2 = mtrStation.corner2 == null ? new Tuples<>(0, 319, 0) : Tuples.fromTuple(mtrStation.corner2);
        newStation.corner1.setY(-64);
        newStation.corner2.setY(319);
        newStation.name = mtrStation.name;
        newStation.color = mtrStation.color;
        return newStation;
    }

    public static Set<KSDStation> fromMTRStations(Set<Station> mtrStations) {
        Set<KSDStation> newStations = new HashSet<>(mtrStations.size());
        mtrStations.forEach(mtrStation -> newStations.add(fromMTRStation(mtrStation)));
        return newStations;
    }

    public KSDStation() {
    }

    public KSDStation(long id, TransportMode transportMode) {
        super(id, transportMode);
    }

    public KSDStation(Map<String, Value> map) {
        super(map);
        MessagePackHelper messagePackHelper = new MessagePackHelper(map);
        zone = messagePackHelper.getInt(KEY_ZONE);
        messagePackHelper.iterateMapValue(KEY_EXITS, (entry) -> {
            List<String> destinations = new ArrayList<>(entry.getValue().asArrayValue().size());
            for(Value destination : entry.getValue().asArrayValue()) {
                destinations.add(destination.asStringValue().asString());
            }
            exits.put(entry.getKey().asStringValue().asString(), destinations);
        });
    }

    public KSDStation(CompoundTag compoundTag) {
        super(compoundTag);
        zone = compoundTag.getInt(KEY_ZONE);
        CompoundTag tagExits = compoundTag.getCompound(KEY_EXITS);
        for(String keyParent : tagExits.getAllKeys()) {
            List<String> destinations = new ArrayList<>();
            CompoundTag tagDestinations = tagExits.getCompound(keyParent);
            for(String keyDestination : tagDestinations.getAllKeys()) {
                destinations.add(tagDestinations.getString(keyDestination));
            }
            exits.put(keyParent, destinations);
        }
    }

    public KSDStation(FriendlyByteBuf packet) {
        super(packet);
        zone = packet.readInt();
        int exitCount = packet.readInt();
        for(int i = 0; i < exitCount; ++i) {
            String parent = packet.readUtf(32767);
            List<String> destinations = new ArrayList<>();
            int destinationCount = packet.readInt();
            for(int j = 0; j < destinationCount; ++j) {
                destinations.add(packet.readUtf(32767));
            }
            exits.put(parent, destinations);
        }
    }

    public void toMessagePack(MessagePacker messagePacker) throws IOException {
        super.toMessagePack(messagePacker);
        messagePacker.packString(KEY_ZONE).packInt(zone);
        messagePacker.packString(KEY_EXITS);
        messagePacker.packMapHeader(exits.size());
        for(Map.Entry<String, List<String>> entry : exits.entrySet()) {
            String key = entry.getKey();
            List<String> destinations = entry.getValue();
            messagePacker.packString(key);
            messagePacker.packArrayHeader(destinations.size());
            for(String destination : destinations) {
                messagePacker.packString(destination);
            }
        }
    }

    public int messagePackLength() {
        return super.messagePackLength() + 2;
    }

    public void writePacket(FriendlyByteBuf packet) {
        super.writePacket(packet);
        packet.writeInt(zone);
        packet.writeInt(exits.size());
        exits.forEach((parent, destinations) -> {
            packet.writeUtf(parent);
            packet.writeInt(destinations.size());
            Objects.requireNonNull(packet);
            destinations.forEach(packet::writeUtf);
        });
    }

    public void update(String key, FriendlyByteBuf packet) {
        switch (key) {
            case KEY_EXIT_EDIT_PARENT -> {
                String oldParent = packet.readUtf(32767);
                String newParent = packet.readUtf(32767);
                setExitParent(oldParent, newParent);
            }
            case KEY_EXIT_DELETE_PARENT -> exits.remove(packet.readUtf(32767));
            case KEY_EXIT_DESTINATIONS -> {
                String parent = packet.readUtf(32767);
                if (parentExists(parent)) {
                    exits.get(parent).clear();
                    int destinationCount = packet.readInt();
                    for (int i = 0; i < destinationCount; ++i) {
                        exits.get(parent).add(packet.readUtf(32767));
                    }
                }
            }
            case KEY_ZONE -> {
                name = packet.readUtf(32767);
                color = packet.readInt();
                zone = packet.readInt();
            }
            default -> super.update(key, packet);
        }
    }

    protected boolean hasTransportMode() {
        return false;
    }

    public Station toMTRStation() {
        Station newStation = new Station(id);
        newStation.zone = zone;
        newStation.exits.putAll(exits);
        newStation.corner1 = corner1 != null ? corner1.toTuple() : new Tuple<>(0, 0);
        newStation.corner2 = corner2 != null ? corner2.toTuple() : new Tuple<>(0, 0);
        newStation.name = name;
        newStation.color = color;
        return newStation;
    }

    public void setZone(Consumer<FriendlyByteBuf> sendPacket) {
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeLong(id);
        packet.writeUtf(transportMode.toString());
        packet.writeUtf(KEY_ZONE);
        packet.writeUtf(name);
        packet.writeInt(color);
        packet.writeInt(zone);
        sendPacket.accept(packet);
    }

    public void setExitParent(String oldParent, String newParent, Consumer<FriendlyByteBuf> sendPacket) {
        setExitParent(oldParent, newParent);
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeLong(id);
        packet.writeUtf(transportMode.toString());
        packet.writeUtf(KEY_EXIT_EDIT_PARENT);
        packet.writeUtf(oldParent);
        packet.writeUtf(newParent);
        sendPacket.accept(packet);
    }

    public void deleteExitParent(String parent, Consumer<FriendlyByteBuf> sendPacket) {
        exits.remove(parent);
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        packet.writeLong(id);
        packet.writeUtf(transportMode.toString());
        packet.writeUtf(KEY_EXIT_DELETE_PARENT);
        packet.writeUtf(parent);
        sendPacket.accept(packet);
    }

    public void setExitDestinations(String parent, Consumer<FriendlyByteBuf> sendPacket) {
        if (parentExists(parent)) {
            FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
            packet.writeLong(id);
            packet.writeUtf(transportMode.toString());
            packet.writeUtf(KEY_EXIT_DESTINATIONS);
            packet.writeUtf(parent);
            packet.writeInt(exits.get(parent).size());
            List<String> var10000 = exits.get(parent);
            Objects.requireNonNull(packet);
            var10000.forEach(packet::writeUtf);
            sendPacket.accept(packet);
        }
    }

    public Map<String, List<String>> getGeneratedExits() {
        List<String> exitParents = new ArrayList<>(exits.keySet());
        exitParents.sort(String::compareTo);
        Map<String, List<String>> generatedExits = new HashMap<>();
        exitParents.forEach((parent) -> {
            String exitLetter = parent.substring(0, 1);
            if (!generatedExits.containsKey(exitLetter)) {
                generatedExits.put(exitLetter, new ArrayList<>());
            }
            generatedExits.get(exitLetter).addAll(exits.get(parent));
            generatedExits.put(parent, exits.get(parent));
        });
        return generatedExits;
    }

    public static long serializeExit(String exit) {
        char[] characters = exit.toCharArray();
        long code = 0L;
        for(char character : characters) {
            code <<= 8;
            code += character;
        }
        return code;
    }

    public static String deserializeExit(long code) {
        StringBuilder exit = new StringBuilder();
        for(long charCodes = code; charCodes > 0L; charCodes >>= 8) {
            exit.insert(0, (char)((int)(charCodes & 255L)));
        }
        return exit.toString();
    }

    private void setExitParent(String oldParent, String newParent) {
        if (parentExists(oldParent)) {
            List<String> existingDestinations = exits.get(oldParent);
            exits.remove(oldParent);
            exits.put(newParent, existingDestinations == null ? new ArrayList<>() : existingDestinations);
        } else {
            exits.put(newParent, new ArrayList<>());
        }
    }

    private boolean parentExists(String parent) {
        return parent != null && exits.containsKey(parent);
    }

    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof KSDStation s) {
            return id == s.id;
        }
        return false;
    }
}
