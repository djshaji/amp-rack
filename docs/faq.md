### What is Amp Rack?
Amp Rack is a Guitar Effects Processor for Android. You can use it to put some effects on your Guitar, voice, or any other instrument.

### But what is What is Amp Rack really?
Amp Rack is a LADSPA Plugin Host for Android. It supports LADSPA SDK v1.1 plugins. Since ANdroid does not (easily) support loading dynamic modules, a lot (!) of plugins (shared files) have been included in the APK. 

Amp Rack takes in audio input, runs it through loaded plugins, and sends output to an audio device, optionally recording it to an audio file.

We use the [Oboe](https://github.com/google/oboe) framework by Google, and Amp Rack achieves Low Latency due to native code and high performance tuning.

### How do I connect my guitar to my phone?
[This is how](https://amprack.acoustixaudio.org/connect.html)

### How do I put effects on my guitar?
Click this button to start the effects engine.

![record](https://user-images.githubusercontent.com/17184025/179359462-cd945362-1def-4b1f-bcd2-100620211fd1.jpg)

Click the + Effect button to show the effect plugins dialog

![add](https://user-images.githubusercontent.com/17184025/179359605-17855b7b-74fe-44b0-9ef9-63074bbad009.jpg)

By default the list shows all available effects. You can add some plugins to your favorites, and toggle the show favourites button to show only _liked_ effects

![show favs](https://user-images.githubusercontent.com/17184025/179359861-73bf2d04-c8d2-44eb-b673-a64b9b3d4ba1.jpg)

You can filter the plugins list to show only one type, such as Distortion, Echo, Reverb, Delay, Chorus etc. A number of plugin types are available.

![plugin_types](https://user-images.githubusercontent.com/17184025/179393179-1861286b-903d-4798-b013-e33b78a68df7.jpg)

Click the + button to add plugin to the effects chain, or ♡ button to add plugin to favourites

![add1](https://user-images.githubusercontent.com/17184025/179393269-17666404-b49a-4197-b3f4-fc8fe6a8be1c.jpg)

### How do I change effect parameters?
Move the sliders to change effect parameters. Labels above parameters indicate name of the parameter.

![params](https://user-images.githubusercontent.com/17184025/179393355-a9be9111-a756-489d-9519-b8ff973e7807.jpg)

### Does changing the sequence of plugins change the sound?
Absolutely! Putting a compress before and after a distortion effect has completely different connotations. Try for yourself and find out!

### How do I record audio?
Click on the record button to enable recording before you turn on the effect engine.

:bulb: You cannot turn recording on or off while the effect engine is running.

:bulb: You can record a backing track with chords or rhythm which can be played on loop. 

![rec](https://user-images.githubusercontent.com/17184025/179393528-8ca73cf4-a0b5-4218-b312-12f857913e27.jpg)

### How do I change the audio recording format?
Go to Settings :left_right_arrow: Export Settings :left_right_arrow: Audio Export Format

### What are presets?
Presets are plugin settings which you can save for use later. When you save a preset, the entire effect engine configuration is saved. 

### How do I use Presets?
Tap the Preset button on the navigation bar to switch to the Preset Tab.

![preset](https://user-images.githubusercontent.com/17184025/179393883-dbc4cd63-ba72-44e5-a108-8fbb7f1eff55.jpg)

### How do I save a preset?
Tap the + Preset button to open the Add Preset Dialog

![add_preset](https://user-images.githubusercontent.com/17184025/179394029-db829289-2e2b-4dd0-8e82-2080e7a0c483.jpg)


The following is saved with a preset:
1. Plugin Parameter Configuration
2. Plugin Sequence

:bulb: You can share a preset publicly by enabling the public option while saving a preset.

### What is the Preset Library?
The Preset Library contains Presets created by users and the Amp Rack team. You can sort the presets by date or popular rating. Use presets from the library to quickly find great sounds for your guitar.

:bulb: _Like_ your favourite presets to quickly find them later and to increase their rating.

### How do I use the Preset Library?
On the Presets screen, tap the Library tab header to switch to the Library tab.

![lib](https://user-images.githubusercontent.com/17184025/179394200-33b13fa6-9288-4bfb-bc95-8cd1049d5736.jpg)

### How do I load a preset from my saved presets or the preset library?
Tap the Big Play Button :play_or_pause_button: to load a preset

![load_p](https://user-images.githubusercontent.com/17184025/179394301-5f6e623b-45d1-434c-9155-afece9c6bc9d.jpg)

### Where are my recorded tracks?
Tap the recordings icon on the navigation bar to switch to the recordings tab.

![rec_t](https://user-images.githubusercontent.com/17184025/179394384-b12473ae-9571-4313-9409-6aac82aa4801.jpg)

### How do I play a recorded track?
Tap a file from the list to play it.

![play](https://user-images.githubusercontent.com/17184025/179394420-d0fb986d-5475-4c8b-bb9b-199b6942c1d4.jpg)

### How do I share a recorded track?
Long press a file to share it.

### How do I play a file as a backing track?
Play a recorded file and turn on the loop option.

![loop](https://user-images.githubusercontent.com/17184025/179395833-85421b37-02ca-408f-9abc-cfabbcca794d.jpg)

### How do I use the Drum Machine?
Tap the Drum Machine icon on the navigation bar to switch to the drum machine tab.

![drums](https://user-images.githubusercontent.com/17184025/179395899-48b8185e-4c43-40a3-a7da-02370e21ab56.jpg)

As with the recordings tab, click on any drum track to play it, and turn on looping to play it on a loop.

### How do I modify app Settings?
From the overflow menu, click on Settings.

![settings](https://user-images.githubusercontent.com/17184025/179396279-c639b08d-ad5a-425e-a027-0d09fc4dab61.jpg)

### How do I change the theme?
Go to Settings :arrow_right: Theme :arrow_right: Background

### How do I set a custom wallpaper?
Go to Settings :arrow_right: Theme :arrow_right: Use a Custom Background

### How do I change input and output audio devices?
Go to Settings :arrow_right: Audio Devices :arrow_right: Input / Output Device

### I plugged in a guitar and headphones, but the audio is playing from my phone mic to the phone speaker.
### I plugged in a USB Audio device / interface, but audio is playing from the phone speaker.
When you insert or remove an audio device, close the app completely, and open it again to make the audio system pick up the new device automatically for playing / recording audio. 

:bulb: You can always set an input / output device manually by going to Settings :arrow_right: Theme :arrow_right: Background

### :egg: How do I open the _hidden_ command prompt?
The app has a built in command prompt which I use for debugging. Long press the app logo on the top left of the main screen to open the command prompt dialog. The dialog has autocomplete and will list all available commands. You can use it as a short cut to quickly open some dialog or change a setting.

![logo](https://user-images.githubusercontent.com/17184025/179396423-3a57514c-ea89-4f6e-93f0-bb6bb0f17aa6.jpg)
