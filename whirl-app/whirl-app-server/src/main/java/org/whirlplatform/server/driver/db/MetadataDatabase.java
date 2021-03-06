package org.whirlplatform.server.driver.db;

import org.apache.empire.db.DBDatabase;
import org.whirlplatform.server.driver.db.table.TableWhirlUserApplications;
import org.whirlplatform.server.driver.db.table.TableWhirlUserGroups;
import org.whirlplatform.server.driver.db.table.TableWhirlUsers;

// org.whirlplatform.server.driver.db.table vs org.whirlplatform.server.driver.db 

public class MetadataDatabase extends DBDatabase {

    private static MetadataDatabase instance;
    private static final long serialVersionUID = 1L;


    public final TableWhirlUsers WHIRL_USERS = new TableWhirlUsers(this);
    public final TableWhirlUserApplications WHIRL_USER_APPLICATIONS = new TableWhirlUserApplications(
            this);
    public final TableWhirlUserGroups WHIRL_USER_GROUPS = new TableWhirlUserGroups(
            this);

    /**
     * Returns the instance of the database.
     *
     * @return
     */
    public static MetadataDatabase get() {
        if (instance == null) {
            instance = new MetadataDatabase();
        }
        return instance;
    }

    /**
     * Default constructor for the MetadataDatabase.
     */
    private MetadataDatabase() {

        // Define foreign key relations

        // foreign key relations done
    }

}
