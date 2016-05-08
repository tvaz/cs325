Readme
============
## LinkLayer should:
- send and recv packets
- with correct sequence numbers
- and send and process ACKs.
- Command codes are implemented
- Checksums are computed

##We aren't as confident that:
- retries actually work
- timeouts are correctly computed


##The layer also does not:
- send or receive beacon frames
- update internal status codes