/* Argyris Patramanis csd4379 */
#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <cmath>
#include <vector>
#include <iomanip>
#include <tbb/tbb.h>
#include <time.h>

using namespace tbb;

struct Object {
    long double x, y, vx, vy, m;
    std::string s;
    struct Quadtree* area; // smallest area that contains object
};

struct Quadtree {
    std::vector<Quadtree*> children;
    long double x, y; // coordinates of the quadrant
    long double size; // size of the quadrant (length)
    int has_object; // number of objects inside the quadrant
    std::vector<Object> object; // vector of objects inside in the quadrant
    long double cmx, cmy; // center of mass (x,y)
};

std::vector<Object> objects; // vector of all objects
int N;
long double size, G, dt;

bool isLeaf(Quadtree* quad) {
    return quad->children.empty();
}

void printFile(std::ofstream& out) {
    out << std::showpoint;
    out << N << "\n";
    out << size << "\n";
    
    for (const auto& obj : objects)
        out << std::setprecision(8) << obj.x << " " << obj.y << " " << obj.vx << " " << obj.vy << " " << obj.m << " " << obj.s << "\n";
}

bool isInsideQuadrant(Quadtree* quad, const Object object) {
    long double edgex1 = quad->x - quad->size / 2;
    long double edgex2 = quad->x + quad->size / 2;

    long double edgey1 = quad->y - quad->size / 2;
    long double edgey2 = quad->y + quad->size / 2;

    return (object.x >= edgex1 && object.x < edgex2 &&
            object.y >= edgey1 && object.y < edgey2);
}

void objectCount(Quadtree* quad) {
    int temp;
    for (int i = 0; i < N; i++) {
        if (isInsideQuadrant(quad, objects[i])) {
            quad->object.push_back(objects[i]);
            quad->has_object++;
            temp = i;
        }
    }

    if(quad->has_object == 1)
        objects[temp].area = quad;
}

void splitSpace(Quadtree* quad) {
    objectCount(quad);

    if (quad->has_object == 0 || quad->has_object == 1)
        return;

    long double quarter_size = quad->size / 4.0;

    for (int i = 0; i < 4; i++) {
        Quadtree *newquad = new Quadtree();
        newquad->x = quad->x + ((i % 2 == 0) ? -quarter_size : quarter_size);
        newquad->y = quad->y + ((i < 2) ? quarter_size : -quarter_size);
        newquad->size = quad->size / 2.0;
        newquad->has_object = 0;
        quad->children.push_back(newquad);

        quad->cmx = 0.0;
        quad->cmy = 0.0;

        splitSpace(newquad);
    }
}

void centerOfMass(Quadtree* quad) {
    if (quad == nullptr)
        return;

    for (const auto& child : quad->children)
        centerOfMass(child);

    if (!isLeaf(quad)) {
        long double totalm = 0.0, totalxm = 0.0, totalym = 0.0;

        for (const auto& obj : quad->object) {
            totalm += obj.m;
            totalxm += obj.m * obj.x;
            totalym += obj.m * obj.y;
        }

        if (totalm != 0) {
            quad->cmx = totalxm / totalm;
            quad->cmy = totalym / totalm;
        } else {
            quad->cmx = quad->x;
            quad->cmy = quad->y;
        }
    }
}

void netForce(Quadtree* quad, Object* object) {
    if (quad == nullptr)
        return;

    if (!isLeaf(quad)) {
        long double r = std::sqrt(std::pow(quad->cmx - object->x, 2) + std::pow(quad->cmy - object->y, 2));

        if (object->area != nullptr) {
            if (r >= object->area->size && !isInsideQuadrant(quad, *object)) { // 1st case
                for (const auto& obj : quad->object) {
                    long double F = G * obj.m * object->m / std::pow(r, 2);
                    long double Fx = F * (quad->cmx - object->x) / r;
                    long double Fy = F * (quad->cmy - object->y) / r;
                    long double ax = Fx / object->m;
                    long double ay = Fy / object->m;

                    object->vx += dt * ax;
                    object->vy += dt * ay;

                    object->x += dt * object->vx;
                    object->y += dt * object->vy;
                }
                return;
            } else { // 2nd case
                for (const auto& child : quad->children)
                    netForce(child, object);
            }
        }
    }
}

int main(int argc, char* argv[]) {
    struct timespec start, end;
    double elapsed;
    std::string line;
    std::ifstream in(argv[1]);
    std::ofstream out(argv[2]);
    int numloops = std::stoi(argv[3]);
    int numthreads = std::stoi(argv[4]);

    if (!in || !out) {
        std::cerr << "Can't open file\n";
        return 0;
    }

    task_scheduler_init init(numthreads);
    clock_gettime(CLOCK_MONOTONIC,&start);

    std::getline(in, line);
    N = std::stoi(line);

    std::getline(in, line);
    size = std::stold(line);

    objects.resize(N);

    for (int i = 0; i < N; i++) {
        std::getline(in, line);
        std::istringstream iss(line);
        iss >> objects[i].x >> objects[i].y >> objects[i].vx >> objects[i].vy >> objects[i].m >> objects[i].s;
    }

    Quadtree* root = new Quadtree();
    root->x = 0.0;
    root->y = 0.0;
    root->size = size * 2.0;
    root->has_object = 0;
    root->cmx = 0.0;
    root->cmy = 0.0;

    G = 6.67 * std::pow(10.0, -11.0);
    dt = 1.0;

    for (int j = 0; j < numloops; j++) {
        splitSpace(root); // serial

        centerOfMass(root); // serial

        parallel_for (blocked_range<size_t>(0, N),
        [=](const blocked_range<size_t>& r) -> void {
            for(size_t i = r.begin(); i != r.end(); i++)
                netForce(root, &objects[i]); // parallel
        });
    }

    clock_gettime(CLOCK_MONOTONIC,&end);
    elapsed=(end.tv_sec-start.tv_sec);
    elapsed += (end.tv_nsec - start.tv_nsec) / 1000000000.0;
    printf("Run time: %fs\n", elapsed);

    printFile(out);

    in.close();
    out.close();
    return 0;
}

