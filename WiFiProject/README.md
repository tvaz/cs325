## Checkpoint 3 Requirements ##
# Mac Rules # DONE
* 802.11 MAC logic implemented
# Address Selectivity # DONE
* Only receive packets sent to local MAC
* Valid source/dest Address
# ACKs #
* Send properly formatted ACK Packet back to the sender
* (No checksums yet)
* don't send acks for the broadcast Address
* Wait the required amount of time before sending ACK
* When transmitting, wait for ack before proceeding
* retransmit according to MAC rules, then move onto next packet
* (make up a fixed timeout value for now)
* You don't have to do media-access steps when sending ACKs
# Sequence Numbers #
* All packets have valid seq numbers
* Numbers start at 0 and maintain a next seq num for each destination
* ACKs carry the seq num of the packet they are acknowledging
* Print a warning to output stream if you detect a gap in the seqnum
* on incoming packets, but accept and queue the data
