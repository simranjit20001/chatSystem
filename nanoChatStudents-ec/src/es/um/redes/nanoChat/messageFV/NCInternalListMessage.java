package es.um.redes.nanoChat.messageFV;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NCInternalListMessage extends NCInternalMessage {

    private final Collection<String> list;
    static protected final String LIST_FIELD = "elements";

    public NCInternalListMessage(byte type, Collection<String> list) {
        super(type);
        this.list = list;
    }

    public List<String> getList() {
        return Collections.unmodifiableList((List<String>) list);
    }

    @Override
    protected StringBuffer toBufferedString() {
        StringBuffer sb = super.toBufferedString();
        String listed = list.stream()
        .map(n -> String.valueOf(n))
        .collect(Collectors.joining(","));
        sb.append(LIST_FIELD+DELIMITER+listed+END_LINE);
        return sb;
    }

    public static NCInternalListMessage readFromString(byte code, String message) {
        Collection<String> list = null;
     
        String[] lines = message.split(String.valueOf(END_LINE));

        int idx = lines[1].indexOf(DELIMITER); // Posición del delimitador
        String field = lines[1].substring(0, idx).toLowerCase(); // minúsculas
        String value = lines[1].substring(idx + 1).trim();

        if (field.equalsIgnoreCase(LIST_FIELD))
            list = Arrays.asList(value.split(","));

        return new NCInternalListMessage(code, list);
    }



}
