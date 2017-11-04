package dk.dren.lightmotion.db.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;

/**
 * A Camera row as stored in the database
 */
@RequiredArgsConstructor
@Getter
public class Camera {
    private final Long id;
    private final Timestamp created;
    private final String name;
    private final String address;
    private final String user;
    private final String password;
    private final int profileNumber;
    private final int lowResProfileNumber;
    private final boolean lowResSnapshot;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Camera camera = (Camera) o;

        if (profileNumber != camera.profileNumber) return false;
        if (lowResProfileNumber != camera.lowResProfileNumber) return false;
        if (lowResSnapshot != camera.lowResSnapshot) return false;
        if (!name.equals(camera.name)) return false;
        if (!address.equals(camera.address)) return false;
        if (user != null ? !user.equals(camera.user) : camera.user != null) return false;
        return password != null ? password.equals(camera.password) : camera.password == null;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + address.hashCode();
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + profileNumber;
        result = 31 * result + lowResProfileNumber;
        result = 31 * result + (lowResSnapshot ? 1 : 0);
        return result;
    }
}
