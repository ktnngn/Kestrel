package protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MessageProtocol {

    public enum MessageType {
        TEXT((byte) 0), PIC((byte) 1), VOICE((byte) 2), FILE((byte) 3);

        public final byte code;
        MessageType(byte code) { this.code = code; }

        public static MessageType fromCode(byte code) {
            for (MessageType t : values()) if (t.code == code) return t;
            throw new IllegalArgumentException("Unknown message type code: " + code);
        }
    }

    public static class Frame {
        public final MessageType type;
        public final byte[] payload;
        public Frame(MessageType type, byte[] payload) {
            this.type = type;
            this.payload = payload;
        }
    }

    public static void writeFrame(DataOutputStream out, MessageType type, byte[] payload) throws IOException {
        out.writeByte(type.code);
        out.writeInt(payload.length);
        out.write(payload);
        out.flush();
    }

    public static Frame readFrame(DataInputStream in) throws IOException {
        byte typeCode = in.readByte();
        int length = in.readInt();
        byte[] payload = new byte[length];
        in.readFully(payload);
        return new Frame(MessageType.fromCode(typeCode), payload);
    }
}