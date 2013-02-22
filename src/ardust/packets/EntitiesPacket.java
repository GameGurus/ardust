package ardust.packets;

import ardust.shared.ByteBufferBuffer;

import java.nio.ByteBuffer;

public class EntitiesPacket extends Packet {
    public ByteBuffer data;

    public EntitiesPacket(ByteBuffer buffer, boolean tosend) {
        data = buffer;
        if (data.remaining() > 20000)
            throw new RuntimeException("Too long of entity delta");
        if (data.remaining() == 0)
            throw new RuntimeException("Whuh?");
    }

    public EntitiesPacket(ByteBuffer buffer) {
        int size = buffer.getShort();
        data = ByteBufferBuffer.alloc(size);
        buffer.get(data.array(), data.arrayOffset(), size);
        data.limit(size);
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.put(packetId());
        buffer.putShort((short)data.remaining());
        buffer.put(data.array(), data.arrayOffset()+ data.position(), data.remaining());
    }

    @Override
    public byte packetId() {
        return Packet.ID_ENTITIES_PACKET;
    }
}