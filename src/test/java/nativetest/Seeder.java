package nativetest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import testdata.Pictures;

/**
 * Creates data with SQL, for the tests that run against the executable: the test cannot use the entities, as the
 * application runs in another process.
 */
public class Seeder implements AutoCloseable {

    private final Connection connection;

    public Seeder(String url, String user, String password) {
        try {
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot connect to the database of the application at " + url, e);
        }
    }

    public long theme(String theme) {
        return insert("insert into TalkTheme (id, color, theme) values (nextval('TalkTheme_SEQ'), 'Blue', ?) returning id",
                theme);
    }

    public long type(String typeEN, String typeFR) {
        return insert("insert into TalkType (id, typeEN, typeFR) values (nextval('TalkType_SEQ'), ?, ?) returning id",
                typeEN, typeFR);
    }

    public long speaker(String firstName, String lastName, String company, int photoSeed) {
        long photo = largeObject(Pictures.photo(photoSeed));
        return insert("insert into Speaker (id, firstName, lastName, company, biography, photo, star, lastUpdated)"
                + " values (nextval('Speaker_SEQ'), ?, ?, ?, 'Biography', ?, false, now()) returning id",
                firstName, lastName, company, photo);
    }

    public long talk(String titleEN, String titleFR, long theme, long type, long... speakers) {
        long talk = insert("insert into Talk (id, titleEN, titleFR, descriptionEN, descriptionFR, isBreak,"
                + " isHiddenInTalksPage, language, level, nbLikes, theme_id, type_id, slidesSecret, lastUpdated)"
                + " values (nextval('Talk_SEQ'), ?, ?, 'Description', 'Description', 'NotABreak', false, 'EN',"
                + " 'Beginner', 0, ?, ?, ?, now()) returning id",
                titleEN, titleFR, theme, type, UUID.randomUUID().toString());
        for (long speaker : speakers) {
            update("insert into talk_speaker (talk_id, speakers_id) values (?, ?)", talk, speaker);
        }
        return talk;
    }

    public long sponsor(String company, String level, int logoSeed) {
        long logo = largeObject(Pictures.logo(logoSeed));
        // the size of the logo, which the application works out when it saves a sponsor
        return insert("insert into Sponsor (id, company, about, aboutEN, level, logo, width, height, lastUpdated)"
                + " values (nextval('Sponsor_SEQ'), ?, 'About', 'About', ?, ?, 200, 80, now()) returning id",
                company, level, logo);
    }

    private long largeObject(byte[] bytes) {
        return insert("select lo_from_bytea(0, ?)", (Object) bytes);
    }

    public long insert(String sql, Object... parameters) {
        try (PreparedStatement statement = prepare(sql, parameters); ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
    }

    public void update(String sql, Object... parameters) {
        try (PreparedStatement statement = prepare(sql, parameters)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
    }

    public String string(String sql, Object... parameters) {
        try (PreparedStatement statement = prepare(sql, parameters); ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getString(1) : null;
        } catch (SQLException e) {
            throw new RuntimeException(sql, e);
        }
    }

    public long count(String sql, Object... parameters) {
        return Long.parseLong(string(sql, parameters));
    }

    private PreparedStatement prepare(String sql, Object... parameters) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
        return statement;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            // nothing to do
        }
    }
}
