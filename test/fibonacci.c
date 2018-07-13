#include <iostream>

using namespace std;

long long fib(int n) {

    if (n == 1)
        return 0;
    if (n == 2)
        return 1;
    
    long long a = 1, b = 2, c;
    int i = 2;
    
    do {
     
        c = a + b;
        a = b + c;
        b = c + a;
        
        i += 3;
        
    } while (i < n);
    
    if (i == n)
        return b;
    if (i - 1 == n)
        return a;
    
    return c;
    

}

int main()
{
    for (int i = 1; i < 20; ++i)
        cout << fib(i) << " ";
}
