package com.vanderfox.sleepsounds

import com.amazon.speech.slu.Intent
import com.amazon.speech.slu.Slot
import com.amazon.speech.speechlet.IntentRequest
import com.amazon.speech.speechlet.LaunchRequest
import com.amazon.speech.speechlet.PlaybackFailedRequest
import com.amazon.speech.speechlet.PlaybackFinishedRequest
import com.amazon.speech.speechlet.PlaybackNearlyFinishedRequest
import com.amazon.speech.speechlet.PlaybackStartedRequest
import com.amazon.speech.speechlet.PlaybackStoppedRequest
import com.amazon.speech.speechlet.Session
import com.amazon.speech.speechlet.SessionEndedRequest
import com.amazon.speech.speechlet.SessionStartedRequest
import com.amazon.speech.speechlet.Speechlet
import com.amazon.speech.speechlet.SpeechletException
import com.amazon.speech.speechlet.SpeechletResponse
import com.amazon.speech.speechlet.SystemExceptionEncounteredRequest
import com.amazon.speech.ui.AudioDirective
import com.amazon.speech.ui.AudioDirectivePlay
import com.amazon.speech.ui.AudioDirectiveStop
import com.amazon.speech.ui.AudioItem
import com.amazon.speech.ui.PlainTextOutputSpeech
import com.amazon.speech.ui.Reprompt
import com.amazon.speech.ui.SimpleCard
import com.amazon.speech.ui.SsmlOutputSpeech
import com.amazon.speech.ui.Stream
import com.amazonaws.services.dynamodbv2.document.ItemCollection
import com.amazonaws.services.dynamodbv2.document.Page
import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.amazonaws.services.dynamodbv2.document.ScanFilter
import com.amazonaws.services.dynamodbv2.document.ScanOutcome
import com.amazonaws.services.dynamodbv2.document.internal.ScanCollection
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec
import com.amazonaws.services.dynamodbv2.model.ScanRequest
import com.amazonaws.services.dynamodbv2.model.ScanResult
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.apache.commons.lang.math.RandomUtils
import org.slf4j.Logger;
import org.slf4j.LoggerFactory
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazon.speech.speechlet.Context

import java.time.LocalTime

/**
 * This app shows how to connect to hero with Spring Social, Groovy, and Alexa.
 * @author Lee Fox and Ryan Vanderwerf
 */
@CompileStatic
public class SleepSoundsSpeechlet implements Speechlet {
    private static final Logger log = LoggerFactory.getLogger(SleepSoundsSpeechlet.class);

    String title = "Sleep Sounds"

    @Override
    SpeechletResponse onPlaybackStarted(PlaybackStartedRequest playbackStartedRequest, Context context) throws SpeechletException {
        return null
    }

    @Override
    SpeechletResponse onPlaybackFinished(PlaybackFinishedRequest playbackFinishedRequest, Context context) throws SpeechletException {
        return null
    }

    @Override
    void onPlaybackStopped(PlaybackStoppedRequest playbackStoppedRequest, Context context) throws SpeechletException {

    }

    @Override
    SpeechletResponse onPlaybackNearlyFinished(PlaybackNearlyFinishedRequest playbackNearlyFinishedRequest, Context context) throws SpeechletException {
        return null
    }

    @Override
    SpeechletResponse onPlaybackFailed(PlaybackFailedRequest playbackFailedRequest, Context context) throws SpeechletException {
        return null
    }

    @Override
    void onSystemException(SystemExceptionEncounteredRequest systemExceptionEncounteredRequest) throws SpeechletException {

    }

    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId())

        session.setAttribute("something", "a session value")

        initializeComponents(session)

        // any initialization logic goes here
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        getWelcomeResponse(session);
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session, Context context)
            throws SpeechletException {
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;
        log.debug("incoming intent:${intentName}")

        switch (intentName) {
            case "PlayRandomSoundIntent":
                playRandomSleepSound(request,session, context)
                break;
            case "PlaySoundIntent":
                playSleepSound(request,session, context)
                break
            case "AMAZON.ResumeIntent":
                resumeEpisode(request,session, context)
                break
            case "AMAZON.HelpIntent":
            case "HelpIntent":
                getHelpResponse()
                break
            case "AMAZON.StopIntent":
            case "AMAZON.CancelIntent":
                stopOrCancelPlayback()
                break
            case "AMAZON.PauseIntent":
                pausePlayback(session,request,context)
                break
            default:
                didNotUnderstand()
                break
        }
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        // any cleanup logic goes here
    }

    /**
     * Creates and returns a {@code SpeechletResponse} with a welcome message.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getWelcomeResponse(final Session session) {
        String speechText = "Welcome to Sleep Sounds Say play sound name or play random sound"

        askResponse(speechText, speechText)
    }


    private SpeechletResponse askResponse(String cardText, String speechText) {
        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle(title);
        card.setContent(cardText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        SpeechletResponse.newAskResponse(speech, reprompt, card);
    }

    private SpeechletResponse tellResponse(String cardText, String speechText) {
        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle(title);
        card.setContent(cardText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        SpeechletResponse.newTellResponse(speech, card);
    }

    private SpeechletResponse askResponseFancy(String cardText, String speechText, String fileUrl) {
        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle(title);
        card.setContent(cardText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);
        log.info("making welcome audio")
        SsmlOutputSpeech fancySpeech = new SsmlOutputSpeech()
        fancySpeech.ssml = "<speak><audio src=\""+fileUrl+"\"/> "+speechText+"</speak>"
        log.info("finished welcome audio")
        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(fancySpeech);

        SpeechletResponse.newAskResponse(fancySpeech, reprompt, card);
    }

    /**
     * Creates a {@code SpeechletResponse} for the help intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getHelpResponse() {
        String speechText = "Say Exit Game or Quit Game to stop the game.  Please follow the prompts I give you, and be sure to speak clearly.";
        askResponse(speechText, speechText)
    }

    private SpeechletResponse didNotUnderstand() {
        String speechText = "I'm sorry.  I didn't understand what you said.  Say help me for help.";
        askResponse(speechText, speechText)
    }

    private SpeechletResponse endGame() {
        String speechText = "OK.  I will stop the game now.  Please try again soon.";
        tellResponse(speechText, speechText)
    }

    /**
     * Initializes the instance components if needed.
     */
    private void initializeComponents(Session session) {
        // initialize any components here like set up a dynamodb connection
    }

    @CompileStatic(TypeCheckingMode.SKIP) // do some meta stuff
    public SpeechletResponse playSleepSound(IntentRequest request, Session session, Context context) {


        log.debug("context:${context}")
        log.debug("context.audioPlayer.playerActivity:${context?.audioPlayer?.playerActivity}")
        log.debug("context.system.application.applicationId:${context?.system?.application?.applicationId}")
        Slot soundNameSlot = request.intent.getSlot("soundName")
        Slot playTimeSlot = request.intent.getSlot("playTime")
        if (!playTimeSlot) {
            playTimeSlot = new Slot("payTime","PT1H") // default to 1 hr
        }

        LocalTime localTime = LocalTime.parse(playTimeSlot.value)
        log.debug("playTime${playTimeSlot.value} localTime=${localTime}")
        log.debug("soundName:"+soundNameSlot.value)
        if (!soundNameSlot?.value) {
            return didNotUnderstand()
        }

        session.setAttribute("soundName",soundNameSlot.value)

        AmazonDynamoDBClient amazonDynamoDBClient
        amazonDynamoDBClient = new AmazonDynamoDBClient()
        DynamoDB dynamoDB = new DynamoDB(amazonDynamoDBClient)

        Table table = dynamoDB.getTable("sleepsounds")
        Item item = table.getItem(new PrimaryKey("soundName", soundNameSlot.value))
        if (item) {
            String speechText = "Starting playback of ${soundNameSlot.value} for ${localTime}"
            Stream audioStream = new Stream()
            audioStream.offsetInMilliseconds = 0
            audioStream.url = item.getString("url")
            audioStream.setToken((request.getRequestId()+audioStream.url).hashCode() as String)
            AudioItem audioItem = new AudioItem(audioStream)

            int playedCount = item.getInt("playedCount")
            playedCount++
            item.withInt("playedCount", playedCount)
            table.putItem(item)

            AudioDirectivePlay audioPlayerPlay = new AudioDirectivePlay(audioItem)
            PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
            speech.setText(speechText)
            // Create the Simple card content.

            SimpleCard card = new SimpleCard()
            card.setTitle(title)
            card.setContent(speechText) //TODO auto retrieve show notes here
            SpeechletResponse.newTellResponse(speech, card, [audioPlayerPlay] as List<AudioDirective>)
        } else {
            String speechText = "I'm sorry I am unable to find the sound ${playTimeSlot.value} to play. Please say Alexa open Sleep Sounds and start over."
            PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
            speech.setText(speechText)
            // Create the Simple card content.
            SimpleCard card = new SimpleCard()
            card.setTitle(title)
            card.setContent(speechText) //TODO auto retrieve show notes here
            SpeechletResponse.newTellResponse(speech, card)
        }

    }

    @CompileStatic(TypeCheckingMode.SKIP) // do some meta stuff
    public SpeechletResponse playRandomSleepSound(IntentRequest request, Session session, Context context) {


        log.debug("context:${context}")
        log.debug("context.audioPlayer.playerActivity:${context?.audioPlayer?.playerActivity}")
        log.debug("context.system.application.applicationId:${context?.system?.application?.applicationId}")

        Slot playTimeSlot = request.intent.getSlot("playTime")
        if (!playTimeSlot) {
            playTimeSlot = new Slot("playTime","PT1H") // default to 1 hr
        }

        //LocalTime localTime = LocalTime.parse(playTimeSlot.value)
        //log.debug("playTime${playTimeSlot.value} localTime=${localTime}")

        AmazonDynamoDBClient amazonDynamoDBClient
        amazonDynamoDBClient = new AmazonDynamoDBClient()
        DynamoDB dynamoDB = new DynamoDB(amazonDynamoDBClient)

        Table table = dynamoDB.getTable("sleepsounds")

        ScanSpec spec = new ScanSpec()
                .withMaxResultSize(100)
                 .withMaxPageSize(100)
        ItemCollection<ScanOutcome> result = table.scan(spec)
        Page soundItems = result.firstPage()

        int tableRowCount = soundItems.size()
        int playTimes = 3
        log.debug("firstPage.size=${tableRowCount}")
        Item item = soundItems.lowLevelResult.items.get(RandomUtils.nextInt(new Random(),tableRowCount))
        if (item) {
            String speechText = "Starting playback of ${item.getString("soundName_nice")}"
            List playItems = new ArrayList<AudioDirectivePlay>(playTimes)
            playTimes.each {
                Stream audioStream = new Stream()
                audioStream.offsetInMilliseconds = 0
                audioStream.url = item.getString("url")
                audioStream.setToken((request.getRequestId() + audioStream.url).hashCode() as String)
                log.debug("playing url:${audioStream.url}")
                AudioItem audioItem = new AudioItem(audioStream)

            int playedCount = item.getInt("playedCount")
            playedCount++
            item.withInt("playedCount", playedCount)
            table.putItem(item)


                AudioDirectivePlay audioPlayerPlay = new AudioDirectivePlay(audioItem)

                playItems.add(audioPlayerPlay)
            }
            // write these to the dyanmo table to pause/resume will work (only way I've found)

            Table stateTable = dynamoDB.getTable("sleepsounds_playback_state")
            Item tokenItem = new Item().withPrimaryKey("token",audioStream.getToken())
                    .withString("streamUrl",playItems.get(0).audioItem.stream.url)
                    .withString("soundsName",item.getString("soundName"))
                    .withNumber("offsetInMillis",0)
                    .withNumber("duration",0)
                    .withNumber("createdDate",System.currentTimeMillis())

            stateTable.putItem(tokenItem)

            PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
            speech.setText(speechText)
            // Create the Simple card content.

            SimpleCard card = new SimpleCard()
            card.setTitle(title)
            card.setContent(speechText) //TODO auto retrieve show notes here
            SpeechletResponse.newTellResponse(speech, card, playItems)
        } else {
            String speechText = "I'm sorry I am unable to find a sound to play. Please say Alexa open Sleep Sounds and start over."
            PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
            speech.setText(speechText)
            // Create the Simple card content.
            SimpleCard card = new SimpleCard()
            card.setTitle(title)
            card.setContent(speechText) //TODO auto retrieve show notes here
            SpeechletResponse.newTellResponse(speech, card)
        }

    }


    @CompileStatic(TypeCheckingMode.SKIP) // do some meta stuff
    public SpeechletResponse resumeEpisode(IntentRequest request, Session session, Context context) {
        log.debug("context:${context}")
        log.debug("context.audioPlayer.playerActivity:${context?.audioPlayer?.playerActivity}")
        log.debug("context.audioPlayer.token:${context?.audioPlayer?.token}")

        AmazonDynamoDBClient amazonDynamoDBClient
        amazonDynamoDBClient = new AmazonDynamoDBClient()
        DynamoDB dynamoDB = new DynamoDB(amazonDynamoDBClient)

        Table table = dynamoDB.getTable("sleepsounds_playback_state")
        Item item = table.getItem(new PrimaryKey("token", context?.audioPlayer?.token))
        if (item) {
            String speechText = "Resuming playback of Sleep Sound ${item.getString("soundName_nice")}"
            Stream audioStream = new Stream()
            audioStream.offsetInMilliseconds = 0

            audioStream.url = item.getString("streamUrl")
            audioStream.setToken(item.getString("token"))
            audioStream.offsetInMilliseconds = item.getNumber("offsetInMillis")
            AudioItem audioItem = new AudioItem(audioStream)

            AudioDirectivePlay audioPlayerPlay = new AudioDirectivePlay(audioItem)
            PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
            speech.setText(speechText)

            SimpleCard card = new SimpleCard()
            card.setTitle(title)
            card.setContent(speechText) //TODO auto retrieve show notes here
            SpeechletResponse.newTellResponse(speech, card, [audioPlayerPlay] as List<AudioDirective>)
        } else {
            String speechText = "I'm sorry I am unable to find your session to resume. Please say Alexa open Sleep Sound and start over."
            PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
            speech.setText(speechText)
            // Create the Simple card content.
            SimpleCard card = new SimpleCard()
            card.setTitle(title)
            card.setContent(speechText) //TODO auto retrieve show notes here
            SpeechletResponse.newTellResponse(speech, card)
        }
    }

    private SpeechletResponse stopOrCancelPlayback() {
        AudioDirectiveStop audioDirectiveClearQueue = new AudioDirectiveStop()
        //audioDirectiveClearQueue.clearBehaviour = "CLEAR_ALL"
        String speechText = "Stopping. GoodBye."
        // Create the Simple card content.
        SimpleCard card = new SimpleCard()
        card.setTitle(title)
        card.setContent(speechText)

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
        speech.setText(speechText)

        // Create reprompt
        Reprompt reprompt = new Reprompt()
        reprompt.setOutputSpeech(speech)

        log.debug("Stopping intent")

        SpeechletResponse.newTellResponse(speech,card,[audioDirectiveClearQueue] as List<AudioDirective>)
    }

    private SpeechletResponse pausePlayback(Session session, IntentRequest request, Context context) {
        log.debug("context:${context}")
        log.debug("context.audioPlayer.playerActivity:${context?.audioPlayer?.playerActivity}")
        log.debug("context.audioPlayer.token:${context?.audioPlayer?.token}")

        AmazonDynamoDBClient amazonDynamoDBClient
        amazonDynamoDBClient = new AmazonDynamoDBClient()
        DynamoDB dynamoDB = new DynamoDB(amazonDynamoDBClient)

        Table table = dynamoDB.getTable("sleepsounds_playback_state")
        try {
            Item item = table.getItem(new PrimaryKey("token", context?.audioPlayer?.token))

            if (item) {
                // update the offset where they paused at
                Item tokenItem = new Item().withPrimaryKey("token", item.getString("token"))
                        .withString("streamUrl", item.getString("streamUrl"))
                        .withString("soundName", item.getString("soundName"))
                        .withNumber("offsetInMillis", context.audioPlayer.offsetInMilliseconds)
                        .withNumber("createdDate", item.getNumber("createdDate"))
                table.deleteItem(new PrimaryKey("token",item.getString("token")))
                table.putItem(tokenItem)
                log.debug("found item: ${item.getString("soundName")}")
            }
        } catch (Exception e) {
            log.debug("Error getting item from dynamo db token:${context?.audioPlayer?.token}")
        }

        AudioDirectiveStop audioDirectiveClearQueue = new AudioDirectiveStop()
        //audioDirectiveClearQueue.clearBehaviour = "CLEAR_ALL"
        String speechText = "Pausing playback. Say resume to restart playback."
        // Create the Simple card content.
        SimpleCard card = new SimpleCard()
        card.setTitle(title)
        card.setContent(speechText)

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech()
        speech.setText(speechText)

        // Create reprompt
        Reprompt reprompt = new Reprompt()
        reprompt.setOutputSpeech(speech)

        log.debug("Pausing intent")

        SpeechletResponse.newTellResponse(speech,card,[audioDirectiveClearQueue] as List<AudioDirective>)
    }
}
