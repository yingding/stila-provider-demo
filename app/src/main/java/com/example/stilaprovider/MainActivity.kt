package com.example.stilaprovider

import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val STILA_PROVIDER_CONTENT_URI: Uri = Uri.parse("content://lmu.pms.stila.provider/stressscore")

    private val projection: Array<String> = arrayOf(
        StilaStressScoreContract.ComputedStressEntry.COLUMN_NAME_TIMESTAMP,    // stressscore table primary key timestamp
        // StilaStressScoreContract.ComputedStressEntry.COLUMN_NAME_MODEL_ID,     // stressscore model id, which shall be 0
        StilaStressScoreContract.ComputedStressEntry.COLUMN_NAME_STRESS_SCORE, // stressscore between 0 and 100
    )
    private val selectionClause: String =
        "${StilaStressScoreContract.ComputedStressEntry.COLUMN_NAME_TIMESTAMP}>=? AND " +
        "${StilaStressScoreContract.ComputedStressEntry.COLUMN_NAME_TIMESTAMP}<=? AND " +
        "${StilaStressScoreContract.ComputedStressEntry.COLUMN_NAME_MODEL_ID}=?"

    private val ASC = "ASC" // increase order
    private val DESC = "DESC" // decrease order
    private val sortOrder = "${StilaStressScoreContract.ComputedStressEntry.COLUMN_NAME_TIMESTAMP} ${ASC}"

    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.text)
    }

    override fun onResume() {
        super.onResume()
        val curUtcTimestampInSecs = System.currentTimeMillis() / 1000L
        val aDayBefore = curUtcTimestampInSecs - (24 * 60 * 60)
        CoroutineScope(Dispatchers.Main).launch {
            textView.text = fetchStilaStressScores(aDayBefore, curUtcTimestampInSecs).toString()
        }
    }

    /**
     * @param beginUTCts Begin timestamp (UTC Timestamp in Seconds)
     * @param endUTCts End timestamp (UTC Timestamp in Seconds)
     */
    private fun fetchStressScores(beginUTCts: Long, endUTCts: Long): List<StressScore> {
        // https://developer.android.com/guide/topics/providers/content-provider-basics#kotlin

        var selectionArgs: Array<String> = arrayOf(
            beginUTCts.toString(),
            endUTCts.toString(),
            StilaStressScoreContract.ComputedStressEntry.BASELINE_STRESS_MODEL_ID
        )
        val mutableList = mutableListOf<StressScore>()

        // https://developer.android.com/guide/topics/providers/content-provider-basics#kotlin
        val mCursor = contentResolver.query(
            STILA_PROVIDER_CONTENT_URI, // The content URI of the stila stressscore table
            projection, // The columns to return for each row
            selectionClause, // Selection criteria
            selectionArgs, // Selection criteria
            sortOrder
        )

        mCursor?.use {cursor ->
            // Determine the column index of the column named "timestamp"
            val tsIdx: Int = cursor.getColumnIndex(StilaStressScoreContract.ComputedStressEntry.COLUMN_NAME_TIMESTAMP)
            val scoreIdx: Int = cursor.getColumnIndex(StilaStressScoreContract.ComputedStressEntry.COLUMN_NAME_STRESS_SCORE)

            while (cursor.moveToNext()) {
                // added new stress score to the list
                mutableList.add(
                    StressScore(cursor.getLong(tsIdx), cursor.getInt(scoreIdx))
                )
            }
            // resource is closed by use block automatically, inside the apply block you will need to close resource manually
            // mCursor.close()
        }

        return mutableList.toList()
    }

    suspend fun fetchStilaStressScores(beginUTCts: Long, endUTCts: Long): List<StressScore> {
        return withContext(Dispatchers.Main) {
            fetchStressScores(beginUTCts, endUTCts)
        }
    }

    data class StressScore(val timestamp: Long, val stressscore: Int)
}