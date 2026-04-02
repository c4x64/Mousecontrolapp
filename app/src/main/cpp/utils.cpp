#include "utils.h"
#include <fstream>
#include <sstream>

std::vector<std::string> split(const std::string &s, char delimiter) {
    std::vector<std::string> tokens;
    std::string token;
    std::istringstream tokenStream(s);
    while (std::getline(tokenStream, token, delimiter)) {
        tokens.push_back(token);
    }
    return tokens;
}

bool fileExists(const std::string &path) {
    std::ifstream f(path.c_str());
    return f.good();
}