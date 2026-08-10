package ph.attendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ph.attendance.ui.AttendanceApp

/**
 * The only activity.
 *
 * Navigation happens inside Compose, so there is no second entry point to secure and nothing
 * exported beyond the launcher itself.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AttendanceApp() }
    }
}
