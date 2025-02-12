from UdpComm import UdpComm
from time import sleep
import json

total_time  = 0;
comm = UdpComm("127.0.0.1", 5005)
behavior_comm = UdpComm("127.0.0.1", 5000)
position_message = json.dumps({'position':{'dy':25}})
lick_message = json.dumps({'lick':{'action':'start'}})
lick_stop_message = json.dumps({'lick':{'action':'stop'}})
reward_message = json.dumps({'valve': {'action': 'open', 'pin': 52}})
frame_delay = 0.02

lick_delay = 1
lick_interval = 5
licking = False
emit_licks = False

while (total_time < 150):
    comm.sendMessage(position_message)
    if emit_licks and total_time > lick_delay and not licking:
        behavior_comm.sendMessage(lick_message)
        behavior_comm.sendMessage(lick_message)
        behavior_comm.sendMessage(lick_message)
        licking = True

    if total_time > lick_delay + 0.5:
        behavior_comm.sendMessage(lick_stop_message)
        lick_delay += lick_interval
        licking = False

    total_time += frame_delay
    sleep(frame_delay)
