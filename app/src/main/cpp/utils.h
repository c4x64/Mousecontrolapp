#ifndef MOUSELOCK_UTILS_H
#define MOUSELOCK_UTILS_H

#include <string>
#include <vector>

std::vector<std::string> split(const std::string &s, char delimiter);
bool fileExists(const std::string &path);

#endif //MOUSELOCK_UTILS_H