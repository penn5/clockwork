package tk.hack5.clockworkpatience

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun newGame(view: View) {
        val intent = Intent(this, PlayActivity::class.java)
        startActivity(intent)
    }
    fun resumeGame(view: View) {
        val intent = Intent(this, PlayActivity::class.java).apply {
            putExtra(PlayActivity.EXTRA_RESUME, true)
        }
        startActivity(intent)
    }
}
