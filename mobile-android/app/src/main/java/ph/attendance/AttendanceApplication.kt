package ph.attendance

import android.app.Application

/**
 * Holds the object graph.
 *
 * Constructed by hand rather than with a dependency-injection framework: the graph is a database,
 * a handful of repositories, and a sync client. A framework would add a compiler plugin and a
 * layer of indirection to solve a problem this app does not have.
 */
class AttendanceApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
