//Asmt 1 Simple C Application
//Tori Vaz
//CS 325
//Jan 27 2016

#include<stdio.h>

//Routine for finding length of a string
int findlength(char *in){
	int len = 0;
	while(in[len]!='\0'){
    	len++;
    }
	return len;
}

//shifts ascii values of each char up by a given increment
void shiftstr(char* in, int increment){
	//find length using prev routine
	int len = findlength(in);
	//Loop and increment each char by given value
	int i = 0;
	while(in[i]>len){
		in[i] = in[i]+increment;
		i++;
	}
}

//main method
int main(int argc, char *argv[])
{
	//get args
    char *str = argv[1];
    int increment;
    //turn incrementer into an int
    increment = atoi(argv[2]);
    //find the length
    int len;
    len = findlength(str);
    //print string pre-shift
    printf("You asked me to shift \"%s\", a string of length %d, by %d.\n", str, len ,increment);
    //shift
    shiftstr(str,increment);
    //print final value
    printf("That gives \"%s\"\n",str);
    return 0;
}