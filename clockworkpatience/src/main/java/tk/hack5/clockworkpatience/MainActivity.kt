package tk.hack5.clockworkpatience

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.SpannableString
import android.text.util.Linkify
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun newGame(@Suppress("UNUSED_PARAMETER") unused: View) {
        val intent = Intent(this, PlayActivity::class.java).apply {
            putExtra(PlayActivity.EXTRA_DISCARD_ON_REVEAL, discard_on_reveal.isChecked)
        }
        startActivity(intent)
    }
    fun resumeGame(@Suppress("UNUSED_PARAMETER") unused: View) {
        val intent = Intent(this, PlayActivity::class.java).apply {
            putExtra(PlayActivity.EXTRA_RESUME, true)
            putExtra(PlayActivity.EXTRA_DISCARD_ON_REVEAL, discard_on_reveal.isChecked)
        }
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.license -> {
                val str = SpannableString(resources.getString(R.string.license))
                Linkify.addLinks(str, Linkify.ALL)
                AlertDialog.Builder(this)
                        .setIcon(R.drawable.currency_usd_off)
                        .setTitle(R.string.license_title)
                        .setMessage(str)
                        .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
