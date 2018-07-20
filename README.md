### BACnetITMobileAuthenticator

<p>A simple mobile authenticator to make authenticating resource constrained devices easier in an Building Automation Network simpley BACnet.</p>

This apps implements a simple out-of-band (OOB) authentication between resource cosntrained device and Bacnet/IT Directory Server (BDS) using omnipresent mobile phones.
<p> Here is the main screen of the app: </p>
<img src="mainScreen.jpg" alt="App Screen" width="250" height="500"></img>

By clicking the Button **ADD DEVICE**, turns the back camera to capture the bliking bits from the LED. 
Before pressing **ADD DEVICE** button, press the push button so device will start blinking LED.
The mobile phone captures the password from the LED blink controlled by the resource constrained device this can be a micro controller this tens of kilo bytes of storage and limited processing power.
The captured password is the one time password which is now delivered from mobile to the server in AddDeviceRequest message(provided the server address hard coded) through existing secure channel (DTLS secured coap channel authenticated using generic PKI certificates).
Once the device blink password is delivered to sever, server will acknowledge the mobile with a SimpleACk message for the AddDeviceRequest, user will press button on the device indicating the device to perform the elliptical curve diffie hellmann key exchange where device and sever exchange their public keys.

The password excanged OOB can be used for Integrity protection as well as means of authentication.
The idea is to avoid PKI based certificates in resource cosntrained devices that are used in building networks.
