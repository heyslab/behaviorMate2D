from UdpComm import UdpComm
from time import sleep, time
import json
import numpy as np

total_time  = 0
comm = UdpComm("127.0.0.1", 5005)
behavior_comm = UdpComm("127.0.0.1", 5000)
pos_message_patch2 = json.dumps({'position':{'xy':(250, 300)}})
pos_message_hw = json.dumps({'position':{'xy':(500, 300)}})
pos_message_patch1 = json.dumps({'position':{'xy':(250, 750)}})

lick_message1 = json.dumps({'lick':{'action':'start', 'pin': 0}})
lick_stop_message1 = json.dumps({'lick':{'action':'stop', 'pin': 0}})
lick_message2 = json.dumps({'lick':{'action':'start', 'pin': 2}})
lick_stop_message2 = json.dumps({'lick':{'action':'stop', 'pin': 2}})
reward_message = json.dumps({'valve': {'action': 'open', 'pin': 52}})
frame_delay = 1
entrance_delay = 1
hw_delay = 1



lick_delay = .13
lick_len = 7 # seconds
lick_interval = 5
licking = False
emit_licks = False

def lick_loop(lick_delay, lick_len, lick_start_msg, lick_stop_msg):
    licks = 0
    while licks < (1/lick_delay * lick_len):

        behavior_comm.sendMessage(lick_start_msg)
        sleep(.01)
        behavior_comm.sendMessage(lick_stop_msg)
        sleep(lick_delay)
        licks += 1
    licks = 0
    sleep(entrance_delay)

patch1 = np.array([50, 780])
hallway2 = np.array([500, 300])
hallway1 = np.array([500, 750])
patch2 = np.array([50, 250])

# Define path
path = [patch1, hallway1, patch1, hallway1, hallway2, patch2, hallway2, hallway1]  # Loop: patch1 → hallway → patch2 → hallway → patch1 ...
step_duration = 1.0  # time (s) to travel between points
frequency = 30  # Hz
steps = int(step_duration * frequency)
interval = 1.0 / frequency

def interpolate_path(start, end, steps):
    return [start + (end - start) * i / steps for i in range(steps)]

# Build full looping trajectory
trajectory = []
for i in range(len(path)):
    start = path[i]
    end = path[(i + 1) % len(path)]
    trajectory.extend(interpolate_path(start, end, steps))

# Loop to send positions
try:
    idx = 0
    while True:
        start_time = time()

        pos = trajectory[idx % len(trajectory)]
        if np.allclose(pos, patch1, atol=1.0):
            sleep(.5)
            lick_loop(lick_delay, 4, lick_message1, lick_stop_message1)
            sleep(.5)

        elif np.allclose(pos, patch2, atol=1.0):
            sleep(.5)
            lick_loop(lick_delay, 4, lick_message2, lick_stop_message2)
            sleep(.5)


        pos_message = json.dumps({'position': {'xy': (float(pos[0]), float(pos[1]))}})
        comm.sendMessage(pos_message)

        idx += 1
        elapsed = time() - start_time
        sleep(max(0, interval - elapsed))

except KeyboardInterrupt:
    print("Stopped.")
# while True:
#     # enter patch 2
#     comm.sendMessage(pos_message_patch2)
#     sleep(entrance_delay)
    
#     # lick
#     lick_loop(lick_delay, 4, lick_message2, lick_stop_message2)
#     sleep(1)
    

#     # enter hallway
#     comm.sendMessage(pos_message_hw)
#     sleep(hw_delay)

#     comm.sendMessage(pos_message_patch2)
#     sleep(entrance_delay)
    
#     # lick
#     lick_loop(lick_delay, 4, lick_message2, lick_stop_message2)
#     sleep(1)

#     # enter patch 1
#     comm.sendMessage(pos_message_patch1)
#     sleep(entrance_delay)

#     # lick
#     lick_loop(lick_delay, 2, lick_message1, lick_stop_message1)

#     comm.sendMessage(pos_message_hw)
#     sleep(hw_delay)


if False:
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
