# Checkpoint 3 Requirements #
### Mac Rules (DONE)
* 802.11 MAC logic implemented
### Address Selectivity (DONE)
* Only receive packets sent to local MAC/broadcast Address
* Valid source/dest Address
### ACKs
* Send properly formatted ACK Packet back to the sender
* don't send acks for the broadcast Address
* Wait the required amount of time before sending ACK
* When transmitting, wait for ack before proceeding
* retransmit according to MAC rules, then move onto next packet
* (make up a fixed timeout value for now)
* You don't have to do media-access steps when sending ACKs
* Packets are dropped after 5 (Dot11 retry limit) attempts
* Transmissions are resent after 3 seconds (taken from average in tests, could not acquire more accurate estimate due to bugs)
### Sequence Numbers
* Packets do not have valid sequence numbers
* Numbers start at 0 and maintain a next seq num for each destination
* ACKs carry the seq num of the packet they are acknowledging
###Known issues
* Code currently crashes when made to send multiple packets at once (known concurrent modification issue, unknown source)
* Code occasionally stalls/crashes after unknown amount of time
* Time outs stop working when connected to network (assumed reason for previous issue)
* Beacons are not sent
* Because of sequence bug, transmissions after the first are typically broken due to being out of sequence
* Limited Buffering is only confirmed for send buffer in non-transmitting environment(receive buffer could not be tested)
* Sliding window does not behave properly (exact behavior unknown)