package chromecast;

import java.util.Hashtable;

public class ApiClientData {

	private static ApiClientData _instance;
    private Hashtable<String, Object> _hash;

    private ApiClientData() {
        _hash = new Hashtable<String, Object>();
    }

    public static ApiClientData getInstance() {
        if(_instance==null) {
            _instance = new ApiClientData();
        }
        return _instance;
    }

    public static void addObjectForKey(String key, Object object) {
        getInstance()._hash.put(key, object);
    }

    public static Object getObjectForKey(String key) {
        ApiClientData client = getInstance();
        Object data = client._hash.get(key);
        return data;
    }
}
