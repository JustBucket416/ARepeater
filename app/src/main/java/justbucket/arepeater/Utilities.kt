package justbucket.arepeater

/**
 * Created by JustBucket on 02-Nov-17.
 */


/**
 * Function to convert milliseconds time to
 * Timer Format
 * Hours:Minutes:Seconds
 */
fun milliSecondsToTimer(milliseconds: Long): String {
    var finalTimerString = ""

    // Convert total duration into time
    val hours = (milliseconds / (1000 * 60 * 60)).toInt()
    val minutes = (milliseconds % (1000 * 60 * 60)).toInt() / (1000 * 60)
    val seconds = (milliseconds % (1000 * 60 * 60) % (1000 * 60) / 1000).toInt()
    // Add hours if there
    if (hours > 0) {
        finalTimerString = "$hours:"
    }

    // Prepending 0 to seconds if it is one digit
    val secondsString = if (seconds < 10) {
        "0$seconds"
    } else {
        "" + seconds
    }

    finalTimerString = "$finalTimerString$minutes:$secondsString"

    // return timer string
    return finalTimerString
}

/**
 * Function to get Progress percentage
 * @param currentDuration - based on the song's progress
 * @param totalDuration   - total duration of the song
 */
fun getProgressPercentage(currentDuration: Long, totalDuration: Long): Int {
    val currentSeconds = (currentDuration / 1000).toInt().toLong()
    val totalSeconds = (totalDuration / 1000).toInt().toLong()

    // return percentage
    return (currentSeconds.toDouble() / totalSeconds * 100).toInt()
}

/**
 * Function to change progress to timer
 * @param progress      - progress of the song
 * @param totalDuration - duration of the song
 * returns current duration in milliseconds
 */
fun progressToTimer(progress: Int, totalDuration: Int): Int {
    // return current duration in milliseconds
    return (progress.toDouble() / 100 * totalDuration / 1000).toInt() * 1000
}
