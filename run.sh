#!/bin/bash 

#TODO: SPECIFY THE HOSTNAMES OF 5 CS MACHINES (lab1-1, cs-2, etc...)
MACHINES=(cs-2 cs-6 cs-4 cs-5)

tmux new-session \; \
	split-window -h \; \
	split-window -v \; \
	split-window -v \; \
	split-window -v \; \
	select-layout main-vertical \; \
	select-pane -t 0 \; \
	send-keys "\"cd $(pwd)/Client > /dev/null; echo -n 'Connected to '; hostname; ./run_client.sh ${MACHINES[3]} middleware\"" C-m \; \
	select-pane -t 2 \; \
	send-keys "ssh -t ${MACHINES[0]} \"cd $(pwd)/Server > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh Flights\"" C-m \; \
	select-pane -t 3 \; \
	send-keys "ssh -t ${MACHINES[1]} \"cd $(pwd)/Server > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh Cars\"" C-m \; \
	select-pane -t 4 \; \
	send-keys "ssh -t ${MACHINES[2]} \"cd $(pwd)/Server > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh Rooms\"" C-m \; \
	select-pane -t 1 \; \
	send-keys "ssh -t ${MACHINES[3]} \"cd $(pwd)/Middleware > /dev/null; echo -n 'Connected to '; hostname; sleep .5s; ./run_middleware.sh middleware ${MACHINES[0]} Flights ${MACHINES[1]} Cars  ${MACHINES[2]} Rooms\"" C-m \; \
