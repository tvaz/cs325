//Asmt 2 HTTP Request in C
//Tori Vaz
//CS 325
//Feb 15 2016

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <netdb.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <arpa/inet.h>

#define PORT "80" // the port client will be connecting to

#define MAXDATASIZE 10000 // max number of bytes we can get at once

// get sockaddr, IPv4 or IPv6:
void *get_in_addr(struct sockaddr *sa)
{
	if (sa->sa_family == AF_INET) {
		return &(((struct sockaddr_in*)sa)->sin_addr);
	}

	return &(((struct sockaddr_in6*)sa)->sin6_addr);
}

int main(int argc, char *argv[])
{
	int sockfd, numbytes;
	char buf[MAXDATASIZE];
	struct addrinfo hints, *servinfo, *p;
	int rv;
	char s[INET6_ADDRSTRLEN];

 if(argc == 1){
   printf("usage: fetch hostname path outfile\n");
   exit(0);
 }
	if (argc != 4) {
	    fprintf(stderr,"usage: client hostname\n");
	    exit(1);
	}

	memset(&hints, 0, sizeof hints);
	hints.ai_family = AF_UNSPEC;
	hints.ai_socktype = SOCK_STREAM;

	if ((rv = getaddrinfo(argv[1], PORT, &hints, &servinfo)) != 0) {
		fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(rv));
		return 1;
	}

	// loop through all the results and connect to the first we can
	for(p = servinfo; p != NULL; p = p->ai_next) {
		if ((sockfd = socket(p->ai_family, p->ai_socktype,
				p->ai_protocol)) == -1) {
			perror("client: socket");
			continue;
		}

		if (connect(sockfd, p->ai_addr, p->ai_addrlen) == -1) {
			perror("client: connect");
			close(sockfd);
			continue;
		}

		break;
	}

	if (p == NULL) {
		fprintf(stderr, "client: failed to connect\n");
		return 2;
	}

	inet_ntop(p->ai_family, get_in_addr((struct sockaddr *)p->ai_addr),
			s, sizeof s);

	printf("Sending GET to %s\n", argv[1]);

	freeaddrinfo(servinfo); // all done with this structure

  char req[80];
	sprintf(req, "GET %s HTTP/1.1\r\nHost: %s\r\n\r\n",argv[2],argv[1]);

  send(sockfd, req, strlen(req), 0);

	if ((numbytes = recv(sockfd, buf, MAXDATASIZE-1, 0)) == -1) {
	    perror("recv");
	    exit(1);
	}

	buf[numbytes] = '\0';

	//i/o business//

	FILE* file = fopen(argv[3],"w");
	char* cutoff = strstr(buf,"\r\n\r\n");
	cutoff+=4;
	numbytes = strlen(cutoff);
	fwrite(cutoff,sizeof(char),numbytes,file);

	printf("Wrote a total of %d bytes to %s\n",numbytes,argv[3]);

	fclose(file);
	close(sockfd);

	return 0;
}
