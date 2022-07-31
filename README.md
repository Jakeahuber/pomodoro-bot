
# Pomodoro Bot

A discord bot for setting study timers (Pomodoros). Study with up to 15 friends and keep track of studying time statistics!



## Installation

Copy this link into your web browser to install the bot on your server. 
```bash
https://discord.com/api/oauth2/authorize?client_id=1002240444861788281&permissions=301184674816&scope=bot
```

## Features

- 6 Pomodoro timers, which can be quit at any time and up to 15 users may join. A server can only run one Pomodoro at a time.
    - 25 minute study, 5 minute break (standard)
    - 50 minute study, 10 minute break (also popular)
    - 15 minute study, 5 minute break
    - 20 minute study, 10 minute break
    - 45 minute study, 15 minute break
    - 55 minute study, 5 minute break
- A counter for how many hours you've studied today.
- A counter for how many hours you've studied since using Pomodoro Bot for the first time.
- Change the time zone associated with your discord account. This is to get a more accurate number for hours studied in a day.



## How to Use

Call **'!pomodoro-info'** to get a list of commands

![pomodoro-info-new](https://user-images.githubusercontent.com/68114979/182041928-65c8e8e1-dca5-4d3c-bc7e-07f16f34875a.png)

Change the time-zone associated with your discord account by calling **'!pomo-set-time'**. This command will only work within a DM with Pomodoro Bot. Changing the time zone is optional, but will make Pomodoro Bot as accurate as possible for tracking how many hours you've studied on a given day.

![set-time-zone-dm](https://user-images.githubusercontent.com/68114979/182042051-73500c80-4018-478d-b84e-d2edbb86192f.png)


Notice that **'!get-time-studied-today'** and **'!get-time-studied'** are currently 0. This will change after running the first Pomodoro timer.

![get-time-studied-initial](https://user-images.githubusercontent.com/68114979/182042143-48ee787a-3bd1-4ff3-9325-3545f7817c10.png)

To start a pomodoro timer, call **'!pomodoro-XX-YY'**, where XX is the length (in minutes) of the studying time and YY is the length (in minutes) of the break time. There are 6 options, which are listed in the features section. For instance, call **'!pomodoro-25-5'** to get a standard Pomodoro timer with a 25 minute study time and a 5 minute break time.

![call-25-5](https://user-images.githubusercontent.com/68114979/182042291-6a201ad1-62c9-40a2-8360-af145d1b021f.png)

During the timer, you can check how much studying time is left by calling **'!pomo-time'**.

![pomo-time](https://user-images.githubusercontent.com/68114979/182042502-0aa026c9-7297-45b3-a42e-c0b88b2e2bcc.png)

After the study timer is over, Pomodoro Bot will ping you.

![break](https://user-images.githubusercontent.com/68114979/182043070-2bc96203-a47d-4b97-885c-2b120fe104de.png)

Once again, you can check how much break time is left by calling **'!pomo-time'**.

![break-time](https://user-images.githubusercontent.com/68114979/182043073-3ebc2550-ab4c-47c4-b762-af4473496a67.png)

After the break timer is over, Pomodoro Bot will ping you again.

![break over](https://user-images.githubusercontent.com/68114979/182043239-22ac65c4-10c0-48fe-9eb3-0061bfb84f79.png)

If you call **'!get-time-studied-today'** and **'!get-time-studied'** again, you'll notice that they both changed from 0 hours to 0.5 hours. This is because the account set a timer for 30 minutes (25 minute study, 5 minute break). If you wait a day and call the command '!get-time-studied-today' again, the result will be 0.0. However, '!get-time-studied' will still be 0.5 hours. 

![get-time-studied-update](https://user-images.githubusercontent.com/68114979/182042410-ee52f0ba-d1b2-4cb4-90d7-acb2cc040994.png)

If a user needs to quit a timer for any reason, they can call **'!quit-pomo'**.

![quit-pomo](https://user-images.githubusercontent.com/68114979/182043322-f833592a-fde8-4a49-8294-45690f405725.png)
## Study with Friends

With the **'!add-to-pomo'** command, up to fifteen users can study using the same Pomodoro!

To get started, have one user create the Pomodoro

![15-5 timer](https://user-images.githubusercontent.com/68114979/182043566-044ac91b-2054-478d-be76-de5597b76d8f.png)

Another user can then join using the command **'!add-to-pomo'**.

![add-to-pomo](https://user-images.githubusercontent.com/68114979/182043581-1d8c9aa7-820e-4656-bdc9-3303ae50369b.png)

Notice test account 2's previous **'!get-time-studied'** and **'!get-time-studied-today'**.

![test-acct-2-time-studied](https://user-images.githubusercontent.com/68114979/182043591-934ae2b3-558b-4bd3-bb1f-dc5ac49113b1.png)

The added user can check how much time is left by calling **'!pomo-time'**. However, the added user cannot call **'!quit-pomo'**. Only the user who created the Pomodoro timer can cancel it.

![user-2-pomo-time](https://user-images.githubusercontent.com/68114979/182043613-2389cdb8-bdf6-4f9f-89bd-17711958efda.png)

When the timers are over, Pomodoro Bot will ping every user who called '!add-to-pomo' and the user who created the Pomodoro.

![break-time-2-ppl](https://user-images.githubusercontent.com/68114979/182044040-0793d5cd-b25d-44d5-976a-0c6b7153c37d.png)

![break-over](https://user-images.githubusercontent.com/68114979/182044170-d983ac58-17b4-4a7c-83e7-15d5882cbe16.png)

The users that call '!add-to-pomo' will also notice their '!get-time-studied' and '!get-time-studied-today' numbers increase.

![time-studied-2ppl](https://user-images.githubusercontent.com/68114979/182044219-cbd8a4f5-5f8f-459c-8d4f-161ec7970696.png)

## Tech

This bot was created using the following technologies:
- The Java Discord API (JDA) for setting up the bot
- MongoDB for storing studying time statistics
- Heroku for deploying the bot onto a server

## Feedback

If you have any feedback, please email me at jakelanhuber@gmail.com or submit an issue to this repository.

