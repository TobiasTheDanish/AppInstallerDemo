package dk.skancode.appinstallerdemo

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import dk.skancode.appinstallerdemo.ui.theme.AppInstallerDemoTheme
import java.io.File

const val PACKAGE_INSTALLED_ACTION =
    "com.example.android.apis.content.SESSION_API_PACKAGE_INSTALLED"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppInstallerDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        val downloadPath = File(baseContext.filesDir, "downloads")
        val file = File(downloadPath, "test.png")
        val fileUri = FileProvider.getUriForFile(baseContext, "dk.skancode.fileprovider", file)

        var session: PackageInstaller.Session? = null
        try {
            val installer = packageManager.packageInstaller
            val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = installer.createSession(sessionParams)
            session = installer.openSession(sessionId)

            session.openWrite("package", 0 , -1).use { packageInSession ->
                file.inputStream().use {
                    val buffer = ByteArray(16384)
                    var n: Int
                    do {
                        n = it.read(buffer)

                        packageInSession.write(buffer, 0, n)

                    } while (n >= 0)
                }
            }

            val intent = Intent(baseContext, this.javaClass).apply {
                action = Intent.ACTION_VIEW
            }
            val pendingIntent = PendingIntent.getActivity(baseContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            val statusReceiver = pendingIntent.intentSender

            session.commit(statusReceiver)
        } catch (e: Exception) {
            throw RuntimeException("Couldn't install package", e)
        } catch (e: RuntimeException) {
            session?.abandon()
            throw e
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val extras = intent.extras
        if (PACKAGE_INSTALLED_ACTION == intent.action) {
            println("Status: ${extras?.getInt(PackageInstaller.EXTRA_STATUS)}")
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AppInstallerDemoTheme {
        Greeting("Android")
    }
}