package es.um.redes.nanoChat.server;

import java.net.InetAddress;
//import java.util.HashSet;
//import java.util.Set;

public class User {

    private final String name;
    private final InetAddress address;
    //private final Set<String> blocked;

    public User(String name, InetAddress address) {
        this.name = name;
        this.address = address;
        //this.blocked = new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public InetAddress getAddress() {
        return address;
    }
/*
    public void block(String name) {
        blocked.add(name);
    }

    public boolean isBlocked(String name) {
        return blocked.contains(name);
    }
*/
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
