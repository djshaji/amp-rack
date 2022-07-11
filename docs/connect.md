# Connecting your guitar to your phone
You don't need to buy expensive (and proprietary) equipment to connect your guitar to your phone. Android is Linux, and everything that works on Linux ought to (and does) work on Android too.

## Connect using Adapters
The simplest way to connect your guitar to your phone is to use adapters. Since the guitar jack is 1/4" TRS and the phone input is 3.5mm 4 way pin, you need two adapters:
1. 1/4" TRS to 3.5mm adapter ($2) [Link on Amazon (Not affiliated)](https://www.amazon.com/Hosa-GMP-386-3-5-TRS-Adaptor/dp/B001CJ68KE/)
2. Two Way 3.5mm splitter to Mic and Headphone

You simply connect your guitar jack to the 1/4" TRS to 3.5mm adapter, and then this adapter to Two Way 3.5mm splitter. Connect a headphone or speakers to this splitter, and connect the splitter to the phone. **Restart the app ** to make it route audio through the splitter automatically, or manually set it by going to **Settings -> Audio Devices**

![connect guitar 1](https://user-images.githubusercontent.com/17184025/178102516-fc47268b-f5f9-446a-925c-77a1451b09a9.png)

## Connect using USB Audio Interface 
You can also connect a guitar (or mic, or another instrument) to your phone using a USB Audio Interface. Because Android is essentially Linux (thankfully with all standard modules compiled in) you can use any HID Audio device with your phone. Amp Rack will automatically pick an external audio interface. You can also manually set input and output audio devices by going to *Settings -> Audio Devices**

![conenct guitar2](https://user-images.githubusercontent.com/17184025/178220325-35196d94-c4d2-41c1-9098-36cf796fd4e2.png)

You can connect using an Audio Interface in the following manner:
1. Connect the interface to your phone **using OTG to USB** and restart Amp Rack.
2. Connect the guitar to the Interface using 1/4" TRS
3. (Optionally) connect headphones to monitor port of Audio Interface (if present)
