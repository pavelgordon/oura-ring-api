import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.jackson.responseObject
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_DATE


class OuraRingApi(private val personalAccessToken: String) {

    private val mapper = ObjectMapper().registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    var format = SimpleDateFormat("yyyy-MM-dd")

    init {
        mapper.propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
        FuelManager.instance.basePath = "https://api.ouraring.com/v1"
        FuelManager.instance.baseParams = listOf("access_token" to personalAccessToken)
    }


    fun getSleep(
        dateFrom: String = now().minusDays(7).format(ISO_DATE),
        dateTo: String = now().format(ISO_DATE)
    ): Pair<List<Summary.Sleep>, Error?> {
        val response = Fuel.get(
            "/sleep", listOf(
                "start" to dateFrom,
                "end" to dateTo
            )
        ).responseObject<Sleeps>(mapper)

        if (response.second.statusCode != 200) {
            return Pair(listOf(), Error(response.second.responseMessage))
        }

        val sleep = response.third.component1()!!.sleep
        return Pair(sleep, null)
    }


    fun getActivity(
        dateFrom: String = now().minusDays(7).format(ISO_DATE),
        dateTo: String = now().format(ISO_DATE)
    ): Pair<List<Summary.Activity>, Error?> {
        val response = Fuel.get(
            "/activity", listOf(
                "start" to dateFrom,
                "end" to dateTo
            )
        ).responseObject<Activities>(mapper)
        if (response.second.statusCode != 200) {
            return Pair(listOf(), Error(response.second.responseMessage))
        }

        return Pair(response.third.component1()!!.activity, null)
    }

    fun getReadiness(
        dateFrom: String = now().minusDays(7).format(ISO_DATE),
        dateTo: String = now().format(ISO_DATE)
    ): Pair<List<Summary.Readiness>, Error?> {
        val response = Fuel.get(
            "/readiness", listOf(
                "start" to dateFrom,
                "end" to dateTo
            )
        ).responseObject<Readiness>(mapper)
        if (response.second.statusCode != 200) {
            return Pair(listOf(), Error(response.second.responseMessage))
        }

        return Pair(response.third.component1()!!.readiness, null)
    }
}

data class Sleeps(val sleep: List<Summary.Sleep>)
data class Activities(val activity: List<Summary.Activity>)
data class Readiness(val readiness: List<Summary.Readiness>)


/**
 * https://cloud.ouraring.com/docs/
 */
sealed class Summary {
    /**
     * Sleep period is a nearly continuous, longish period of time spent lying down in bed. For each sleep period it detects, Oura ring performs sleep analysis and stores a set of measurement parameters that summarize the period. The ring calculates the sleep period specific parameters within four hours from the period end, but sleep analysis is always triggered when you open the application.
     */
    data class Sleep(
        /**
         * One day prior to the date when the sleep period ended. Note: this is one day before the date that is shown in the apps.
         */
        var summaryDate: String,
        /**
         * Index of the sleep period among sleep periods with the same summary_date, where 0 = first sleep period of the day.
         */
        val periodId: Int,
        val isLongest: Int,
        /**
         * Unit: Minutes
        Timezone offset from UTC as minutes. For example, EEST (Eastern European Summer Time, +3h) is 180. PST (Pacific Standard Time, -8h) is -480. Note that timezone information is also available in the datetime values themselves, see for example.bedtime_start
         */
        val timezone: Int,
        /**
         * Local time when the sleep period started
         */
        val bedtimeStart: String,
        /**
         * Local time when the sleep period ended.
         */
        val bedtimeEnd: String,
        /**
         * Range: 1-100, or 0 if not available.
        Sleep score represents overall sleep quality during the sleep period. It is calculated as a weighted average of sleep score contributors that represent one aspect of sleep quality each. The sleep score contributor values are also available as separate parameters.
         */
        val score: Int,
        /**
         * Range: 1-100, or 0 if not available.
        Represents total sleep time's (see sleep.total) contribution for sleep quality. The value depends on age of the user - the younger, the more sleep is needed for good score. The weight of sleep.score_total in sleep score calculation is 0.35.
         */
        val scoreTotal: Int,
        /**
         * Range: 1-100, or 0 if not available.
        Represents sleep disturbances' contribution for sleep quality. Three separate measurements are used to calculate this contributor value:

        Wake-up count - the more wake-ups, the lower the score.
        Got-up count - the more got-ups, the lower the score.
        Restless sleep (sleep.restless) - the more motion detected during sleep, the lower the score.
        Each of these three values has weight 0.05 in sleep score calculation, giving sleep.score_disturbances total weight of 0.15.
         */
        val scoreDisturbances: Int,
        /**
         * Range: 1-100, or 0 if not available.
        Represents sleep efficiency's (see sleep.efficiency) contribution for sleep quality. The higher efficiency, the higher score. The weight of sleep.score_efficiency in sleep score calculation is 0.10.
         */
        val scoreEfficiency: Int,
        /**
         * Range: 1-100, or 0 if not available.
        Represents sleep onset latency's (see sleep.onset_latency) contribution for sleep quality. A latency of about 15 minutes gives best score. Latency longer than that many indicate problems falling asleep, whereas a very short latency may be a sign of sleep debt. The weight of sleep.score_latency in sleep score calculation is 0.10.
         */
        val scoreLatency: Int,
        /**
         * Range: 1-100, or 0 if not available.
        Represents REM sleep time's (see sleep.rem) contribution for sleep quality. The value depends on age of the user - the younger, the more sleep REM is needed for good score. The weight of sleep.score_rem in sleep score calculation is 0.10.
         */
        val scoreRem: Int,
        /**
         * Range: 1-100, or 0 if not available.
        Represents deep (N3) sleep time's (see sleep.deep) contribution for sleep quality. The value depends on age of the user - the younger, the more sleep is needed for good score. The weight of sleep.score_deep in sleep score calculation is 0.10.
         */
        val scoreDeep: Int,
        /**
         * Range: 1-100, or 0 if not available.
        Represents circadian alignment's contribution for sleep score. Sleep midpoint time (sleep.midpoint_time) between 12PM and 3AM gives highest score. The more the midpoint time deviates from that range, the lower the score. The weigh of sleep.score_alignment in sleep score calculation is 0.10.
         */
        val scoreAlignment: Int,
        /**
         * Unit: Seconds
         * Total amount of sleep registered during the sleep period (sleep.total = sleep.rem + sleep.light + sleep.deep).
         */
        val total: Int,
        /**
         * Unit: Seconds
         * Total duration of the sleep period (sleep.duration = sleep.bedtime_end - sleep.bedtime_start).
         */
        val duration: Int,
        /**
         * Unit: Seconds
         * Total amount of awake time registered during the sleep period.
         */
        val awake: Int,
        /**
         * Total amount of light (N1 or N2) sleep registered during the sleep period.
         */
        val light: Int,
        /**
         * Total amount of REM sleep registered during the sleep period.
         */
        val rem: Int,
        /**
         * Total amount of deep (N3) sleep registered during the sleep period.
         */
        val deep: Int,
        /**
         * Unit: seconds
        Detected latency from bedtime_start to the beginning of the first five minutes of persistent sleep.
         */
        val onsetLatency: Int,
        /**
         * Unit: %
        Restlessness of the sleep time, i.e. percentage of sleep time when the user was moving.
         */
        val restless: Int,
        /**
         * Range: 0-100%
        Sleep efficiency is the percentage of the sleep period spent asleep (100% * sleep.total / sleep.duration).
         */
        val efficiency: Int,
        /**
         * Unit: seconds
        The time in seconds from the start of sleep to the midpoint of sleep. The midpoint ignores awake periods.
         */
        val midpointTime: Int,
        /**
         * Unit: beats per minute
        The lowest heart rate (5 minutes sliding average) registered during the sleep period.
         */
        val hrLowest: Int,
        /**
         * Unit: beats per minute
         * The average heart rate registered during the sleep period.
         */
        val hrAverage: Double,
        /**
         * Unit: milliseconds
        The average HRV calculated with rMSSD method.
         */
        val rmssd: Int,
        /**
         * Unit: breaths per minute
        Average respiratory rate.
         */
        val breathAverage: Int,
        /**
         * Unit: Celsius
        Skin temperature deviation from the long-term temperature average.
         */
        val temperatureDelta: Double,
        /**
         * A string that contains one character for each starting five minutes of the sleep period, so that the first period starts from sleep.bedtime.start: - '1' = deep (N3) sleep - '2' = light (N1 or N2) sleep - '3' = REM sleep - '4' = awake
         * Example: "443432222211222333321112222222222111133333322221112233333333332232222334"
         */
        @JsonProperty("hypnogram_5min")
        val hypnogram5Min: String,
        /**
         * Unit: beats per minute
         * Average heart rate for each beginning 5 minutes of the sleep period, the first period starting from sleep.bedtime_start.
         */
        @JsonProperty("hr_5min")
        val hr5Min: List<Int>,
        /**
         * Unit: milliseconds
        The average HRV (calculated using rMSSD method) for each beginning 5 minutes of the sleep period, the first period starting from sleep.bedtime_start.
         */
        @JsonProperty("rmssd_5min")
        val rmssd5Min: List<Int>,
    )

    data class Activity(
        /**
         * Date when the activity period started. Oura activity period is from 4 AM to 3:59 AM user's local time.
         */
        var summaryDate: String,
        /**
         * UTC time when the activity day began. Oura activity day is usually from 4AM to 4AM local time.
         */
        var dayStart: String,//datetime?
        /**
         * Format: Date time UTC time when the activity day ended. Oura activity day is usually from 4AM to 4AM local time.
         */
        var dayEnd: String,
        /**
         * Unit: Minutes
        Timezone offset from UTC as minutes. For example, EEST (Eastern European Summer Time, +3h) is 180. PST (Pacific Standard Time, -8h) is -480. Note that timezone information is also available in the datetime values themselves, see for example.bedtime_start
         */
        var timezone: Int,
        /**
         * Range: 1-100, or 0 if not available.
        Activity score provides an estimate how well recent physical activity has matched ring user's needs. It is calculated as a weighted average of activity score contributors that represent one aspect of suitability of the activity each. The contributor values are also available as separate parameters.
         */
        var score: Int,
        /**
         * Range: 1-100, or 0 if not available.
        This activity score contributor indicates how well the ring user has managed to avoid of inactivity (sitting or standing still) during last 24 hours. The more inactivity, the lower contributor value.

        The contributor value is 100 when inactive time during past 24 hours is below 5 hours. The contributor value is above 95 when inactive time during past 24 hours is below 7 hours.

        The weight of activity.score_stay_active in activity score calculation is 0.15.
         */
        var scoreStayActive: Int,
        /**
         * Range: 1-100, or 0 if not available.
        This activity score contributor indicates how well the ring user has managed to avoid long periods of inactivity (sitting or standing still) during last 24 hours. The contributor includes number of continuous inactive periods of 60 minutes or more (excluding sleeping). The more long inactive periods, the lower contributor value.

        The contributor value is 100 when no continuous inactive periods of 60 minutes or more have been registered. The contributor value is above 95 when at most one continuous inactive period of 60 minutes or more has been registered.

        The weight of activity.score_move_every_hour in activity score calculation is 0.10.
         */
        var scoreMoveEveryHour: Int,
        /**
         * Range: 1-100, or 0 if not available.
        This activity score contributor indicates how often the ring user has reached his/her daily activity target during seven last days (100 = six or seven times, 95 = five times).

        The weight of activity.score_meet_daily_targets in activity score calculation is 0.25.
         */
        var scoreMeetDailyTargets: Int,
        /**
         * Range: 1-100, or 0 if not available.
        This activity score contributor indicates how regularly the ring user has had physical exercise the ring user has got during last seven days.

        The contributor value is 100 when the user has got more than 100 minutes of medium or high intensity activity on at least four days during past seven days. The contributor value is 95 when the user has got more than 100 minutes of medium or high intensity activity on at least three days during past seven days.

        The weight of activity.score_training_frequency in activity score calculation is 0.10.
         */
        var scoreTrainingFrequency: Int,
        /**
         * Range: 1-100, or 0 if not available.
        This activity score contributor indicates how much physical exercise the ring user has got during last seven days.

        The contributor value is 100 when thes sum of weekly MET minutes is over 2000. The contributor value is 95 when the sum of weekly MET minutes is over 750. There is a weighting function so that the effect of each day gradually disappears.

        The weight of activity.score_training_volume in activity score calculation is 0.15.
         */
        var scoreTrainingVolume: Int,
        /**
         * Range: 1-100, or 0 if not available.
        This activity score contributor indicates if the user has got enough recovery time during last seven days.

        The contributor value is 100 when: 1. The user has got at least two recovery days during past 7 days. 2. No more than two days elapsed after the latest recovery day.

        The contributor value is 95 when: 1. The user has got at least one recovery day during past 7 days. 2. No more than three days elapsed after the latest recovery day.

        Here a day is considered as a recovery day when amount of high intensity activity did not exceed 100 MET minutes and amount of medium intensity activity did not exceed 200 MET minutes. The exact limits will be age and gender dependent.

        The weight of activity.score_recovery_time in activity score calculation is 0.25.
         */
        var scoreRecoveryTime: Int,
        /**
         * Unit: meters
        Daily physical activity as equal meters i.e. amount of walking needed to get the same amount of activity.
         */
        var dailyMovement: Int,
        /**
         * Unit: minutes
        Number of minutes during the day when the user was not wearing the ring. Can be used as a proxy for data accuracy, i.e. how well the measured physical activity represents actual total activity of the ring user.
         */
        var nonWear: Int,
        /**
         * Unit: minutes
        Number of minutes during the day spent resting i.e. sleeping or lying down (average MET level of the minute is below 1.05).
         */
        var rest: Int,
        /**
         * Unit: minutes
        Number of inactive minutes (sitting or standing still, average MET level of the minute between 1.05 and 2) during the day.
         */
        var inactive: Int,
        /**
         * Type: Int
        Number of continuous inactive periods of 60 minutes or more during the day.
         */
        var inactivityAlerts: Int,
        /**
         * Unit: minutes
        Number of minutes during the day with low intensity activity (e.g. household work, average MET level of the minute between 2 and age dependent limit).
         */
        var low: Int,
        /**
         * Unit: minutes
        Number of minutes during the day with medium intensity activity (e.g. walking). The upper and lower MET level limits for medium intensity activity depend on user's age and gender.
         */
        var medium: Int,
        /**
         * Unit: minutes
        Number of minutes during the day with high intensity activity (e.g. running). The lower MET level limit for high intensity activity depends on user's age and gender.
         */
        var high: Int,
        /**
         * Total number of steps registered during the day.
         */
        var steps: Int,
        /**
         * kilocalories
        Total energy consumption during the day including Basal Metabolic Rate in kilocalories.
         */
        var calTotal: Int,
        /**
         * Unit: kilocalories
        Energy consumption caused by the physical activity of the day in kilocalories.
         */
        var calActive: Int,
        /**
         *  MET minutes
        Total MET minutes accumulated during inactive minutes of the day.
         */
        var metMinInactive: Int,
        /**
         * Unit: MET minutes
        Total MET minutes accumulated during low intensity activity minutes of the day.
         */
        var metMinLow: Int,
        /**
         * Unit: MET minutes
        Total MET minutes accumulated during medium and high intensity activity minutes of the day.
         */
        var metMinMediumPlus: Int,
        /**
         * Unit: MET minutes
        Total MET minutes accumulated during medium intensity activity minutes of the day.
         */
        var metMinMedium: Int,
        /**
         * Unit: MET minutes
        Total MET minutes accumulated during high intensity activity minutes of the day.
         */
        var metMinHigh: Int,
        /**
         * Type: Float
        Average MET level during the whole day.
         */
        var averageMet: Double,
        /**
         * Type: String
        A string that contains one character for each starting five minutes of the activity period, so that the first period starts from 4 AM local time:

        0: Non-wear
        1: Rest (MET level below 1.05)
        2: Inactive (MET level between 1.05 and 2)
        3: Low intensity activity (MET level between 2 and age/gender dependent limit)
        4: Medium intensity activity
        5: High intensity activity
        Example: 1112211111111111111111111111111111111111111111233322322223333323322222220000000000000000000000000000000000000000000000000000000233334444332222222222222322333444432222222221230003233332232222333332333333330002222222233233233222212222222223121121111222111111122212321223211111111111111111
         */
        @JsonProperty("class_5min")
        var class5Min: String,
        /**
         * Average MET level for each minute of the activity period, starting from 4 AM local time.
         */
        @JsonProperty("met_1min")
        var met1Min: List<Double>,
        /**
         * Range: 0-4
        Note: Missing for days before Rest Mode was available.
        Indicates whether Rest Mode was enabled or recently enabled. The Rest Mode state can be one of five states:

        0: Off
        1: Entering Rest Mode
        2: Rest Mode
        3: Entering recovery
        4: Recovering
         */
        var restModeState: Int
    )

    /**
     * Readiness tells how ready you are for the day. A Readiness Score above 85% indicates that you're well recovered. A score below 70% usually means that an essential Readiness Contributor, such as Body Temperature or previous night's sleep, falls outside your normal range, or clearly differs from recommended, science-based values.
     */
    data class Readiness(
        /**
         * One day prior to the date when the sleep period (that this readiness score takes into account) ended. Note: this is one day before the date that is shown in the apps.
         */
        var summaryDate: String,
        /**
         * Index of the sleep period among sleep periods with the same summary_date, where 0 = first sleep period of the day. Each readinesss calculation is associated with a sleep period.
         */
        var periodId: Int,
        /**
         * Range: 1-100, or 0 if not available.
         */
        var score: Int,
        /**
         * Range: 1-100, or 0 if not available.
         */
        var scorePreviousNight: Int,
        /**
         * Range: 1-100, or 0 if not available.
         */
        var scoreSleepBalance: Int,
        /**
         * Range: 1-100, or 0 if not available.
         */
        var scorePreviousDay: Int,
        /**
         * Range: 1-100, or 0 if not available.
         */
        var scoreActivityBalance: Int,
        /**
         * Range: 1-100, or 0 if not available.
         */
        var scoreRestingHr: Int,
        /**
         * Range: 1-100, or 0 if not available.
         * Note: May be missing. Not available for days before HRV was part of readiness score.
         */
        var scoreHrvBalance: Int,
        /**
         * Range: 1-100, or 0 if not available.
         */
        var scoreRecoveryIndex: Int,
        /**
         * Range: 1-100, or 0 if not available.
         */
        var scoreTemperature: Int,
        /**
         * Range: 0-4
         * Note: Missing for days before Rest Mode was available. <br/>
         * Indicates whether Rest Mode was enabled or recently enabled. The Rest Mode state can be one of five states:
         * 0: Off
         * 1: Entering Rest Mode
         * 2: Rest Mode
         * 3: Entering recovery
         * 4: Recovering
         */
        var restModeState: Int,
    )
}